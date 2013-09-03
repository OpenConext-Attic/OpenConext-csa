/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.surfnet.coin.csa.api.cache;


import nl.surfnet.coin.csa.dao.LmngIdentifierDao;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.domain.MappingEntry;
import nl.surfnet.coin.csa.model.License;
import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.service.CrmService;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CrmCacheTest {

  private CrmCache cache;
  private CrmService service;
  private LmngIdentifierDao dao;

  @Before
  public void before() throws Exception {
    cache = new CrmCache();

    cache.setDelay(0);
    cache.setDuration(1000);
    service = mock(CrmService.class);
    cache.setCrmService(service);

    dao = mock(LmngIdentifierDao.class);
    cache.setLmngIdentifierDao(dao);

    when(dao.findAllIdentityProviders()).thenReturn(getIdentityProviders());
    when(dao.findAllServiceProviders()).thenReturn(getServiceProviders());

    List<License> licenses = new ArrayList<License>();
    licenses.add(createLicense());
    when(service.getLicensesForIdpAndSp(any(IdentityProvider.class), anyString())).thenReturn(licenses);
    List<Article> articles = new ArrayList();
    articles.add(createArticle());

    when(service.getArticlesForServiceProviders(anyListOf(String.class))).thenReturn(articles);

    cache.afterPropertiesSet();

    //cache needs to kick in
    Thread.sleep(250);
  }


  @Test
  public void testGetLicense() {
    Service service = new Service();
    service.setSpEntityId("spId-2");
    License license = cache.getLicense(service, "idpId-2");

    assertNotNull(license);
  }

  @Test
  public void testGetArticle() {
    Service service = new Service();
    service.setSpEntityId("spId-2");
    Article article = cache.getArticle(service);

    assertNotNull(article);
  }

  @Test
  public void noArticleFound() {
    when(service.getArticlesForServiceProviders(anyListOf(String.class))).thenReturn(Collections.<Article>emptyList());
    when(dao.findAllServiceProviders()).thenReturn(getProviders("spId"));
    cache.doPopulateCache();
    assertNull(cache.getArticle(new Service(1L, "name", "", "", true, "", "spId-0")));
  }

  private License createLicense() {
    Date now = new Date();
    return new License(now, now, "licenseNumber", "institutionName");
  }

  private Article createArticle() {
    Article article = new Article("lmngIdentifier");
    return article;
  }

  private List<MappingEntry> getIdentityProviders() {
    return getProviders("idpId");
  }

  private List<MappingEntry> getServiceProviders() {
    return getProviders("spId");
  }

  private List<MappingEntry> getProviders(final String providerType) {
    List<MappingEntry> identityProviders = new ArrayList<MappingEntry>();
    for (int i = 0; i < 3; i++) {
      identityProviders.add(new MappingEntry(providerType + "-" + i, "lmngId-" + i));
    }
    return identityProviders;
  }


}
