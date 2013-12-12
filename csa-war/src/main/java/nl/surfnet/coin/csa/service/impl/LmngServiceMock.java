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
package nl.surfnet.coin.csa.service.impl;

import nl.surfnet.coin.csa.domain.Account;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.model.License;
import nl.surfnet.coin.csa.service.CrmService;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.List;

/**
 * LicensingServiceMock.java
 */
@SuppressWarnings("unused")
public class LmngServiceMock implements CrmService {

  private ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

  private List<Article> articles;
  private License license;
  private List<Account> institutions;
  private List<Account> services;

  @SuppressWarnings("unchecked")
  public LmngServiceMock() {
    try {
      TypeReference<List<Article>> articleTypeReference = new TypeReference<List<Article>>() {
      };
      this.articles = (List<Article>) parseJsonData(articleTypeReference, "lmng-json/articles.json");
      TypeReference<License> licenseTypeReference = new TypeReference<License>() {
      };
      this.license = (License) parseJsonData(licenseTypeReference, "lmng-json/licenses.json");
      TypeReference<List<Account>> accountTypeReference = new TypeReference<List<Account>>() {
      };
      this.institutions = (List<Account>) parseJsonData(accountTypeReference, "lmng-json/institutions.json");
      this.services = (List<Account>) parseJsonData(accountTypeReference, "lmng-json/services.json");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Object parseJsonData(TypeReference<?> typeReference, String jsonFile) {
    try {
      return objectMapper.readValue(new ClassPathResource(jsonFile).getInputStream(), typeReference);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void setDebug(boolean debug) {
  }

  public void setEndpoint(String endpoint) {
  }


  @Override
  public String performQuery(String rawQuery) {
    throw new RuntimeException("performQuery not implemented by " + getClass().getName());
  }

  @Override
  public void evictCache() {
  }


  @Override
  public String getInstitutionName(String guid) {
    return null;
  }

  @Override
  public String getServiceName(String lmngId) {
    Article article = getService(lmngId);
    return article == null ? null : article.getArticleName();
  }
  
  @Override
  public Article getService(String guid) {
    for (Article current : articles) {
      if (current.getLmngIdentifier().equals(guid)) {
        return current;
      }
    }
    return null;
  }

  @Override
  public List<Account> getAccounts(boolean isInstitution) {
    return isInstitution ? institutions : services;
  }

  @Override
  public List<License> getLicensesForIdpAndSp(IdentityProvider identityProvider, String articleIdentifier) {
    return Arrays.asList(license);
  }

  @Override
  public List<License> getLicensesForIdpAndSps(IdentityProvider identityProvider, List<String> articleIdentifiers) throws LmngException {
    return Arrays.asList(license);
  }

  @Override
  public List<Article> getArticlesForServiceProviders(List<String> serviceProviderEntityIds) {
    return articles.subList(0, Math.min(articles.size(), serviceProviderEntityIds.size()));
  }

}
