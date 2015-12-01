/*
 * Copyright 2012 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csa.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Preconditions;

import csa.domain.Account;
import csa.domain.Article;
import csa.service.CrmService;
import csa.dao.LmngIdentifierDao;
import csa.domain.IdentityProvider;
import csa.model.License;

/**
 * Implementation of a licensing service that get's it information from a
 * webservice interface on LMNG
 */
public class LmngServiceImpl implements CrmService {

  private static final Logger log = LoggerFactory.getLogger(LmngServiceImpl.class);

  private static final String PATH_FETCH_QUERY_GET_INSTITUTION = "lmngqueries/lmngQueryGetInstitution.xml";

  private final LmngIdentifierDao lmngIdentifierDao;

  private CrmUtil lmngUtil = new LmngUtil();

  private final boolean debug;
  private final String endpoint;

  public LmngServiceImpl(LmngIdentifierDao lmngIdentifierDao, boolean debug, String endpoint) {
    this.lmngIdentifierDao = lmngIdentifierDao;
    this.debug = debug;
    this.endpoint = endpoint;
  }

  @Cacheable(value = "crm")
  @Override
  public List<License> getLicensesForIdpAndSp(IdentityProvider identityProvider, String articleIdentifier) {
    List<License> result = new ArrayList<>();
    Preconditions.checkNotNull(identityProvider);
    Preconditions.checkNotNull(articleIdentifier);

    try {
      String lmngInstitutionId = getLmngIdentityId(identityProvider);

      if (lmngInstitutionId == null || lmngInstitutionId.trim().length() == 0) {
        return result;
      }

      // apparently LMNG has a problem retrieving licenses when there has been a revision to the underlying agreement
      // yields the license. For this reason, we have two extra queries that we do when no licenses are found
      for (final CrmUtil.LicenseRetrievalAttempt attempt : CrmUtil.LicenseRetrievalAttempt.values()) {
        // get the file with the soap request
        String soapRequest = lmngUtil.getLmngSoapRequestForIdpAndSp(lmngInstitutionId, Arrays.asList(articleIdentifier), new Date(), endpoint, attempt);
        if (debug) {
          lmngUtil.writeIO("lmngRequest", StringEscapeUtils.unescapeHtml4(soapRequest));
        }
        // call the webservice
        String webserviceResult = getWebServiceResult(soapRequest);
        // read/parse the XML response to License objects
        result = lmngUtil.parseLicensesResult(webserviceResult, debug);
        if (result.size() > 0) { // as soon as we have a result, return it
          return result;
        }
      }
    } catch (Exception e) {
      log.error("Exception while retrieving licenses for article " + articleIdentifier, e);
      return Collections.emptyList();
    }
    return result;
  }

  @Cacheable(value = "crm")
  @Override
  public List<Article> getArticlesForServiceProviders(List<String> serviceProvidersEntityIds) throws LmngException {
    List<Article> result = new ArrayList<>();
    try {
      Map<String, String> serviceIds = getLmngServiceIds(serviceProvidersEntityIds);

      // validation, we need at least one serviceId
      if (CollectionUtils.isEmpty(serviceIds)) {
        return result;
      }

      // get the file with the soap request
      String soapRequest = lmngUtil.getLmngSoapRequestForSps(serviceIds.keySet(), endpoint);
      if (debug) {
        lmngUtil.writeIO("lmngRequest", StringEscapeUtils.unescapeHtml4(soapRequest));
      }

      // call the webservice
      String webserviceResult = getWebServiceResult(soapRequest);
      // read/parse the XML response to License objects
      List<Article> parsedArticles = lmngUtil.parseArticlesResult(webserviceResult, debug);

      for (Article article : parsedArticles) {
        article.setServiceProviderEntityId(serviceIds.get(article.getLmngIdentifier()));
        result.add(article);
      }
    } catch (Exception e) {
      String exceptionMessage = String.format("Error retrieving articlesForServiceProviders. SP ids: %s", serviceProvidersEntityIds.toString());
      log.error(exceptionMessage, e);
      throw new LmngException(exceptionMessage, e);
    }
    return result;
  }

  @Cacheable(value = "crm")
  @Override
  public String getServiceName(String guid) {
    Article article = getService(guid);
    return article == null ? null : article.getArticleName();
  }

  @Cacheable(value = "crm")
  @Override
  public Article getService(final String guid) {
    Article result = null;
    try {
      // get the file with the soap request
      String soapRequest = lmngUtil.getLmngSoapRequestForSps(Arrays.asList(guid), endpoint);
      if (debug) {
        lmngUtil.writeIO("lmngRequest", StringEscapeUtils.unescapeHtml4(soapRequest));
      }

      // call the webservice
      String webserviceResult = getWebServiceResult(soapRequest);
      // read/parse the XML response to License objects
      List<Article> resultList = lmngUtil.parseArticlesResult(webserviceResult, debug);
      if (resultList != null && resultList.size() > 0) {
        result = resultList.get(0);
      }
    } catch (Exception e) {
      log.error("Exception while retrieving article/license", e);
    }
    return result;
  }

  @Override
  public List<Account> getAccounts(boolean isInstitution) {
    List<Account> accounts = new ArrayList<>();
    try {
      // get the file with the soap request
      String soapRequest = lmngUtil.getLmngSoapRequestForAllAccount(isInstitution, endpoint);
      if (debug) {
        lmngUtil.writeIO("lmngRequest", StringEscapeUtils.unescapeHtml4(soapRequest));
      }
      // call the webservice
      String webserviceResult = getWebServiceResult(soapRequest);
      // read/parse the XML response to Account objects
      accounts = lmngUtil.parseAccountsResult(webserviceResult, debug);
    } catch (Exception e) {
      log.error("Exception while retrieving article/license", e);
    }
    return accounts;
  }

  @Override
  @Cacheable(value = "crm")
  public String getInstitutionName(String guid) {
    String result = null;
    try {
      ClassPathResource queryResource = new ClassPathResource(PATH_FETCH_QUERY_GET_INSTITUTION);

      // Get the soap/fetch envelope
      String soapRequest = lmngUtil.getLmngRequestEnvelope();

      InputStream inputStream;
      inputStream = queryResource.getInputStream();
      String query = IOUtils.toString(inputStream);
      query = query.replaceAll(LmngUtil.INSTITUTION_IDENTIFIER_PLACEHOLDER, guid);

      // html encode the string
      query = StringEscapeUtils.escapeHtml4(query);

      // Insert the query in the envelope and add a UID in the envelope
      soapRequest = soapRequest.replaceAll(LmngUtil.QUERY_PLACEHOLDER, query);
      soapRequest = soapRequest.replaceAll(LmngUtil.ENDPOINT_PLACEHOLDER, endpoint);
      soapRequest = soapRequest.replaceAll(LmngUtil.UID_PLACEHOLDER, UUID.randomUUID().toString());

      if (debug) {
        lmngUtil.writeIO("lmngRequestInstitution", StringEscapeUtils.unescapeHtml4(soapRequest));
      }

      String webserviceResult = getWebServiceResult(soapRequest);
      result = lmngUtil.parseResultInstitute(webserviceResult, debug);
    } catch (Exception e) {
      log.error("Exception while retrieving article/license with GUID: " + guid, e);
    }
    return result;
  }

  /**
   * Get the response from the webservice call (using credentials and endpoint
   * address from this class settings) after executing the given soapRequest
   * string.
   *
   * @param soapRequest A string representation of the soap request
   * @return an inputstream of the webservice response
   * @throws IOException
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableKeyException
   * @throws KeyManagementException
   */
  protected String getWebServiceResult(final String soapRequest) throws IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
    log.debug("Calling the LMNG proxy webservice, endpoint: {}", endpoint);

    HttpPost httppost = new HttpPost(endpoint);
    httppost.setProtocolVersion(HttpVersion.HTTP_1_1);
    httppost.setHeader("Content-Type", "application/soap+xml;charset=UTF-8");
    httppost.setEntity(new StringEntity(soapRequest));

    RequestConfig requestConfig = RequestConfig.custom().setExpectContinueEnabled(false).build();
    HttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

    long beforeCall = System.currentTimeMillis();
    HttpResponse httpResponse = httpclient.execute(httppost);
    long afterCall = System.currentTimeMillis();
    log.debug("LMNG proxy webservice called in {} ms. Http response: {}", afterCall - beforeCall, httpResponse);

    HttpEntity httpresponseEntity = httpResponse.getEntity();

    // Continue only if we have a successful response (code 200)
    int status = httpResponse.getStatusLine().getStatusCode();
    // Get String representation of response
    String stringResponse = IOUtils.toString(httpresponseEntity.getContent());

    if (debug) {
      lmngUtil.writeIO("lmngWsResponseStatus" + status, StringEscapeUtils.unescapeHtml4(stringResponse));
    }

    if (status != 200) {
      log.debug("LMNG webservice response content is:\n{}", stringResponse);
      throw new RuntimeException("Invalid response from LMNG webservice. Http response " + httpResponse);
    }

    // Close the entity's InputStream, as prescribed.
    httpresponseEntity.getContent().close();

    return stringResponse;
  }

  /**
   * Get the LMNG identifier for the given IDP
   */
  private String getLmngIdentityId(IdentityProvider identityProvider) {
    // currently institutionId can be null, so check first
    if (identityProvider != null && identityProvider.getInstitutionId() != null) {
      return lmngIdentifierDao.getLmngIdForIdentityProviderId(identityProvider.getInstitutionId());
    }
    return null;
  }

  /**
   * Get the LMNG identifier for the given SP
   */
  private String getLmngServiceId(String serviceProviderEntityId) {
    if (serviceProviderEntityId != null) {
      return lmngIdentifierDao.getLmngIdForServiceProviderId(serviceProviderEntityId);
    }
    return null;
  }

  /**
   * Get the LMNG identifiers for the given SP list
   *
   * @return a map with the LMNGID as key and serviceprovider entity ID as value
   */
  private Map<String, String> getLmngServiceIds(List<String> serviceProvidersEntityIds) {
    Map<String, String> result = new HashMap<>();
    for (String spId : serviceProvidersEntityIds) {
      String serviceId = getLmngServiceId(spId);
      if (serviceId != null) {
        result.put(serviceId, spId);
      }
    }
    return result;
  }

  @Override
  public String performQuery(String rawQuery) {
    try {
      String soapRequest = lmngUtil.getLmngRequestEnvelope();
      String query = StringEscapeUtils.escapeHtml4(rawQuery);
      soapRequest = soapRequest.replaceAll(LmngUtil.QUERY_PLACEHOLDER, query);
      soapRequest = soapRequest.replaceAll(LmngUtil.ENDPOINT_PLACEHOLDER, endpoint);
      soapRequest = soapRequest.replaceAll(LmngUtil.UID_PLACEHOLDER, UUID.randomUUID().toString());

      return getWebServiceResult(soapRequest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @CacheEvict(value = "crm", allEntries = true)
  public void evictCache() {
  }

  public void setLmngUtil(CrmUtil lmngUtil) {
    this.lmngUtil = lmngUtil;
  }
}
