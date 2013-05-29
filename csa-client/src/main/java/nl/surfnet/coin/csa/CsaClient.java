/*
 * Copyright 2013 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.surfnet.coin.csa;

import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.model.Taxonomy;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for the CSA API.
 */
public class CsaClient implements Csa {

  private static final Logger LOG = LoggerFactory.getLogger(CsaClient.class);

  /**
   * OAuth2 Client Key (from the JS oauth2 client when this client was registered
   */
  @Value("${csa.client.key}")
  private String csaClientKey;

  /**
   * OAuth2 Client Secret (from the JS oauth2 client when this client was registered
   */
  @Value("${csa.client.secret}")
  private String csaClientSecret;

  /**
   * Location of the API
   */
  @Value("${csa.oauth2.authorization.url}")
  private String csaOAuth2AuthorizationUrl;

  /**
   * Location of the OAuth2 Authorization Server to retrieve the Access Token (client credentials)
   */
  @Value("${csa.base.url}")
  private String csaBaseLocation;

  private String accessToken;

  private RestTemplate restTemplate = new RestTemplate();

  public CsaClient() {
  }

  public CsaClient(String csaBaseLocation, String csaOAuth2AuthorizationUrl, String csaClientKey, String csaClientSecret) {
    this.csaBaseLocation = csaBaseLocation;
    this.csaOAuth2AuthorizationUrl = csaOAuth2AuthorizationUrl;
    this.csaClientKey = csaClientKey;
    this.csaClientSecret = csaClientSecret;
    this.accessToken = getAccessToken();
  }

  @Override
  public List<Service> getPublicServices() {
    return getFromCsa("/api/public/services.json", null, Service[].class);
  }

  @Override
  public List<Service> getProtectedServices() {
    return getFromCsa("/api/protected/services.json", null, Service[].class);
  }

  @Override
  public List<Service> getServicesForIdp(String idpEntityId) {
    String url = "/api/protected/idp/services.json?idpEntityId={idpEntityId}";
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    return (List<Service>) getFromCsa(url, variables, Service[].class);
  }

  @Override
  public Service getServiceForIdp(String id, long serviceId) {
    // FIXME
    return null;
  }

  @Override
  public Taxonomy getTaxonomy() {
    return getFromCsa("/api/public/taxonomy.json", null, Taxonomy.class);
  }

  @Override
  public List<Action> getJiraActions() {
    return getFromCsa("/api/protected/actions.json", null, Action[].class);
  }

  @Override
  public Action createAction(Action action) {
    return getFromCsa("/api/protected/action.json", action, Action.class);
  }

  @Override
  public Service getService(long id) {
    String location = "/api/protected/services/{id}.json";
    Map variables = new HashMap<String, String>();
    variables.put("id", id);
    return (Service) getFromCsa(location, variables, Service.class);
  }

  @Override
  public void setCsaBaseLocation(String csaBaseLocation) {
    this.csaBaseLocation = csaBaseLocation;
  }

  private <T> T getFromCsa(String url, Class clazz) {
    return getFromCsa(url, null, null, clazz);
  }

  private <T> T getFromCsa(String url, Object bodyJson, Class clazz) {
    return getFromCsa(url, null, bodyJson, clazz);
  }

  private <T> T getFromCsa(String url, Map<String, ?> variables, Class clazz) {
    return getFromCsa(url, variables, null, clazz);
  }

  private <T> T getFromCsa(String url, Map<String, ?> variables, Object bodyJson, Class clazz) {
    return doGetFromCsa(url, variables, bodyJson, clazz, true);
  }

  private <T> T doGetFromCsa(String url, Map<String, ?> variables, Object bodyJson, Class clazz, boolean retry) {
    HttpHeaders headers = new HttpHeaders();
    if (accessToken == null) {
      accessToken = getAccessToken();
    }
    headers.add("Authorization", "bearer " + accessToken);

    HttpEntity requestEntity;
    HttpMethod method;
    if (bodyJson != null) {
      requestEntity = new HttpEntity<Object>(bodyJson, headers);
      method = HttpMethod.POST;
    } else {
      requestEntity = new HttpEntity(headers);
      method = HttpMethod.GET;
    }

    ResponseEntity<T> response;

    if (CollectionUtils.isEmpty(variables)) {
      response = restTemplate.exchange(URI.create(csaBaseLocation + url), method, requestEntity, clazz);
    } else {
      response = restTemplate.exchange(csaBaseLocation + url, method, requestEntity, clazz, variables);
    }
    if (retry && response.getStatusCode() != HttpStatus.OK) {
      //let's try again with a new AccessToken
      accessToken = null;
      doGetFromCsa(url, variables, bodyJson, clazz, false);
    }
    T body = response.getBody();
    if (clazz.isArray()) {
      return getListResult((T[]) body);
    }
    return body;
  }

  /*
   *  (T) Arrays.<T>asList(body) won't work as the type is not inferred and we end up with a list containing one entry: the array
   */
  private <T> T getListResult(T[] body) {
    List<T> result = new ArrayList<T>();
    T[] arr = body;
    for (T t : arr) {
      result.add(t);
    }
    return (T) result;
  }

  /*
   * This could be achieved using the methods we use for Csa REST calls, but it would make that implementation needless generic (e.g. complex)
   */
  private String getAccessToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + new String(Base64.encodeBase64((csaClientKey + ":" + csaClientSecret).getBytes())));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<String> requestEntity = new HttpEntity<String>("grant_type=client_credentials", headers);
    ResponseEntity<Map> response = restTemplate.exchange(URI.create(csaOAuth2AuthorizationUrl),
            HttpMethod.POST,
            requestEntity,
            Map.class);
    if (response.getStatusCode() != HttpStatus.OK) {
      LOG.error("Received HttpStatus {} when trying to obtain AccessToken", response.getStatusCode());
      return null;
    } else {
      Map map = response.getBody();
      return (String) map.get("access_token");
    }

  }

}
