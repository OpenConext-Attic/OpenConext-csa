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

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import nl.surfnet.coin.csa.model.*;
import nl.surfnet.coin.oauth.OauthClient;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Client for the CSA API.
 */
public class CsaClient implements Csa {

  private static final Logger LOG = LoggerFactory.getLogger(CsaClient.class);

  private OauthClient oauthClient;
  /**
   * ObjectMapper to log responses in the same format they arrived.
   */
  private ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

  /**
   * Location of the CSA API
   */
  private String csaBaseLocation;

  private String defaultLocale = "en";

  public CsaClient() {
  }

  public CsaClient(String csaBaseLocation) {
    this.csaBaseLocation = csaBaseLocation;
  }

  @Override
  public List<Service> getPublicServices() {
    return restoreCategoryReferences((List<Service>) oauthClient.exchange(csaBaseLocation + "/api/public/services.json?lang={lang}", getLocaleVariable(), Service[].class));
  }

  private Map getLocaleVariable() {
    Map variables = new HashMap<String, String>();
    variables.put("lang", getLocale());
    return variables;
  }

  @Override
  public List<Service> getProtectedServices() {
    return restoreCategoryReferences((List<Service>) oauthClient.exchange(csaBaseLocation + "/api/protected/services.json?lang={lang}", getLocaleVariable(), Service[].class));
  }

  @Override
  public List<Service> getServicesForIdp(String idpEntityId) {
    String url = csaBaseLocation + "/api/protected/idp/services.json?idpEntityId={idpEntityId}&lang={lang}";
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    variables.put("lang", getLocale());
    return restoreCategoryReferences((List<Service>) oauthClient.exchange(url, variables, Service[].class));
  }

  @Override
  public List<OfferedService> findOfferedServicesFor(String idpEntityId) {
    return new ArrayList<>();
  }

  @Override
  public Service getServiceForIdp(String idpEntityId, long serviceId) {
    String location = csaBaseLocation + "/api/protected/services/{serviceId}.json?idpEntityId={idpEntityId}&lang={lang}";
    Map variables = new HashMap<String, String>();
    variables.put("serviceId", serviceId);
    variables.put("idpEntityId", idpEntityId);
    variables.put("lang", getLocale());
    Service service = (Service) oauthClient.exchange(location, variables, Service.class);
    service.restoreCategoryReferences();
    return service;
  }

  @Override
  public Service getServiceForIdp(String idpEntityId, String spEntityId) {
    String url = csaBaseLocation + "/api/protected/service.json?idpEntityId={idpEntityId}&spEntityId={spEntityId}&lang={lang}";
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    variables.put("spEntityId", spEntityId);
    variables.put("lang", getLocale());
    Service service = (Service) oauthClient.exchange(url, variables, Service.class);
    service.restoreCategoryReferences();
    return service;
  }

  @Override
  public Taxonomy getTaxonomy() {
    Taxonomy taxonomy = (Taxonomy) oauthClient.exchange(csaBaseLocation + "/api/public/taxonomy.json?lang={lang}",getLocaleVariable(), Taxonomy.class);
    List<Category> categories = taxonomy.getCategories();
    for (Category category : categories) {
      List<CategoryValue> values = category.getValues();
      for (CategoryValue value : values) {
        value.setCategory(category);
      }
    }
    return taxonomy;
  }

  @Override
  public List<Action> getJiraActions(String idpEntityId) {
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    return (List<Action>) oauthClient.exchange(csaBaseLocation + "/api/protected/actions.json?idpEntityId={idpEntityId}", variables, Action[].class);
  }

  @Override
  public void clearProviderCache() {
    oauthClient.exchange(csaBaseLocation + "/api/protected/cache/clear.json", String.class);
  }

  public Action createAction(Action action) {
    return (Action) oauthClient.exchange(csaBaseLocation + "/api/protected/action.json", action, Action.class);
  }

  @Override
  public List<InstitutionIdentityProvider> getInstitutionIdentityProviders(String identityProviderId) {
    return (List<InstitutionIdentityProvider>) oauthClient.exchange(csaBaseLocation + "/api/protected/identityproviders.json?identityProviderId={identityProviderId}",
            Collections.singletonMap("identityProviderId", identityProviderId),
            InstitutionIdentityProvider[].class);
  }

  @Override
  public List<InstitutionIdentityProvider> getAllInstitutionIdentityProviders() {
    return (List<InstitutionIdentityProvider>) oauthClient.exchange(csaBaseLocation + "/api/protected/all-identityproviders.json",
            InstitutionIdentityProvider[].class);
  }

  @Override
  public void setCsaBaseLocation(String csaBaseLocation) {
    this.csaBaseLocation = csaBaseLocation;
  }

  @Override
  public Statistics getStatistics(int month, int year) {
    if (month < 0 || month > 12 || year < 0 ) {
      throw new IllegalArgumentException("invalid call for statistics with month " + month + " and year " + year);
    }
    return (Statistics) oauthClient.exchange(csaBaseLocation + "/api/protected/stats.json?month="+month+"&year="+year, Statistics.class);
  }

  public void setOauthClient(OauthClient oauthClient) {
    this.oauthClient = oauthClient;
  }

  private List<Service> restoreCategoryReferences(List<Service> services) {
    for (Service service : services) {
      service.restoreCategoryReferences();
    }
    return services;
  }

  /*
  * Note: this is a deliberate design choice. We want to be able to transparently call Csa services without passing in HttpServletRequest and / or Locale.
  */
  private String getLocale() {
    Locale locale = null;
    ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (sra != null) {
      HttpServletRequest request = sra.getRequest();
      if (request != null) {
        locale = RequestContextUtils.getLocale(request);
      }
    }
    return locale != null ? locale.getLanguage() : defaultLocale;
  }

}