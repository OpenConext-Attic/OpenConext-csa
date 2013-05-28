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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.model.Taxonomy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Client for the CSA API.
 */
public class CsaClient implements Csa {

  private static final Logger LOG = LoggerFactory.getLogger(CsaClient.class);

  /**
   * Location of the API
   */
  private String csaBaseLocation;

  private RestTemplate tpl = new RestTemplate();

  public CsaClient() {
  }

  public CsaClient(String csaBaseLocation) {
    this.csaBaseLocation = csaBaseLocation;
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
    return getFromCsa("/api/protected/history.json", null, Action[].class);
  }

  @Override
  public Service getService(long id) {
    String location = "/api/protected/services/{id}.json";
    Map variables = new HashMap<String, String>();
    variables.put("id", id);
    return (Service) getFromCsa(location, variables, Service.class);
  }

  private <T> T getFromCsa(String url, Map<String, ?> variables, Class clazz) {
    ResponseEntity<T> entity;
    if (CollectionUtils.isEmpty(variables)) {
      entity = tpl.getForEntity(csaBaseLocation + url, clazz);
    } else {
      entity = tpl.getForEntity(csaBaseLocation + url, clazz, variables);
    }
    T body = entity.getBody();
    if (clazz.isArray()) {
      List<T> result = new ArrayList<T>();
      //(T) Arrays.<T>asList(body) won't work as the type is not inferred and we end up with s list containing an array
      T[] arr = (T[]) body;
      for (T t : arr) {
        result.add(t);
      }
      return (T) result;
    }
    return body;
  }

  @Override
  public void setCsaBaseLocation(String csaBaseLocation) {
    this.csaBaseLocation = csaBaseLocation;
  }
}
