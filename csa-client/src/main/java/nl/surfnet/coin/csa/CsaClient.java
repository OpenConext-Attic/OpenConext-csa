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
import nl.surfnet.coin.csa.model.InstitutionIdentityProvider;
import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.model.Taxonomy;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.*;

import java.net.URI;
import java.util.*;
import java.io.IOException;

/**
 * Client for the CSA API.
 */
public class CsaClient implements Csa {

  private static final Logger LOG = LoggerFactory.getLogger(CsaClient.class);

  /**
   * ObjectMapper to log responses in the same format they arrived.
   */
  private ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

  /**
   * OAuth2 Client Key (from the JS oauth2 client when this client was registered)
   */
  private String csaClientKey;

  /**
   * OAuth2 Client Secret (from the JS oauth2 client when this client was registered
   */
  private String csaClientSecret;

  /**
   * Location of the OAuth2 Authorization Server to retrieve the Access Token (client credentials)
   */
  private String apisOAuth2AuthorizationUrl;

  /**
   * Location of the CSA API
   */
  private String csaBaseLocation;

  private String accessToken;

  private RestTemplate restTemplate = new RestTemplate();

  public CsaClient() {
  }

  public CsaClient(String csaBaseLocation, String apisOAuth2AuthorizationUrl, String csaClientKey, String csaClientSecret) {
    this.csaBaseLocation = csaBaseLocation;
    this.apisOAuth2AuthorizationUrl = apisOAuth2AuthorizationUrl;
    this.csaClientKey = csaClientKey;
    this.csaClientSecret = csaClientSecret;
  }

  @Override
  public List<Service> getPublicServices() {
    return getFromCsa("/api/public/services.json", Service[].class);
  }

  @Override
  public List<Service> getProtectedServices() {
    return getFromCsa("/api/protected/services.json", Service[].class);
  }

  @Override
  public List<Service> getServicesForIdp(String idpEntityId) {
    String url = "/api/protected/idp/services.json?idpEntityId={idpEntityId}";
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    return (List<Service>) getFromCsa(url, variables, Service[].class);
  }

  @Override
  public Service getServiceForIdp(String idpEntityId, long serviceId) {
      String location = "/api/protected/services/{serviceId}.json?idpEntityId={idpEntityId}";
      Map variables = new HashMap<String, String>();
      variables.put("serviceId", serviceId);
      variables.put("idpEntityId", idpEntityId);
      return (Service) getFromCsa(location, variables, Service.class);
  }
  
  @Override
  public Service getServiceForIdp(String idpEntityId, String spEntityId) {
    String url = "/api/protected/idp/service.json?idpEntityId={idpEntityId}&spEntityId={spEntityId}";
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    variables.put("spEntityId", spEntityId);
    return (Service) getFromCsa(url, variables, Service.class);
  }

  @Override
  public Taxonomy getTaxonomy() {
    return (Taxonomy) getFromCsa("/api/public/taxonomy.json", Taxonomy.class);
  }

  @Override
  public List<Action> getJiraActions(String idpEntityId) {
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    return (List<Action>) getFromCsa("/api/protected/actions.json?idpEntityId={idpEntityId}", variables, Action[].class);
  }

  public Action createAction(Action action) {
    return (Action) getFromCsa("/api/protected/action.json", action, Action.class);
  }

  @Override
  public List<InstitutionIdentityProvider> getInstitutionIdentityProviders(String identityProviderId) {
    return (List<InstitutionIdentityProvider>) getFromCsa("/api/protected/identityproviders.json?identityProviderId={identityProviderId}",
            Collections.singletonMap("identityProviderId", identityProviderId),
            InstitutionIdentityProvider[].class);
  }

  @Override
  public void setCsaBaseLocation(String csaBaseLocation) {
    this.csaBaseLocation = csaBaseLocation;
  }

  private <T> T getFromCsa(String url, Class clazz) {
    return getFromCsa(url, null, clazz);
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

    String fullUrl = csaBaseLocation + url;

    LOG.debug("Will send {}-request to {}, with parameters {} and body: {}", method.name(), fullUrl, variables, bodyJson);

    ResponseEntity<T> response;

    try {
      if (CollectionUtils.isEmpty(variables)) {
        response = restTemplate.exchange(URI.create(fullUrl), method, requestEntity, clazz);
      } else {
        response = restTemplate.exchange(fullUrl, method, requestEntity, clazz, variables);
      }
    } catch (HttpClientErrorException clientException) {
      if (clientException.getStatusCode() == HttpStatus.FORBIDDEN && retry) {
        LOG.info("Got a 'forbidden' response. Will retry with a new access token. HTTP status: {}", clientException.getMessage());
        accessToken = null;
        return doGetFromCsa(url, variables, bodyJson, clazz, false);
      } else {
        LOG.info("Error during request to CSA. Response body: {}", clientException.getResponseBodyAsString());
        throw clientException;
      }
    } catch (HttpServerErrorException serverException) {
      LOG.info("Error during request to CSA. Response body: {}", serverException.getResponseBodyAsString());
      throw serverException;
    }


    T body = response.getBody();

    if (LOG.isDebugEnabled()) {
      try {
        LOG.debug("Response: {}", objectMapper.writeValueAsString(body));
      } catch (IOException e) {
        LOG.info("Could not serialize response object for logging: {}", e.getMessage());
      }
    }

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
    try {
      ResponseEntity<Map> response = restTemplate.exchange(URI.create(apisOAuth2AuthorizationUrl),
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
    } catch (RestClientException e) {
      LOG.error("Error trying to obtain AccessToken", e);
      return null;
    }

  }

}
