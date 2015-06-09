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
package csa.api.cache;


import csa.domain.MappingEntry;
import csa.service.CrmService;
import csa.dao.LmngIdentifierDao;
import csa.domain.Article;
import csa.domain.IdentityProvider;
import csa.model.License;
import csa.model.Service;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertNotNull;
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

    service = mock(CrmService.class);
    dao = mock(LmngIdentifierDao.class);

    when(dao.findAllIdentityProviders()).thenReturn(getIdentityProviders());
    when(dao.findAllServiceProviders()).thenReturn(getServiceProviders());

    List<License> licenses = new ArrayList<>();
    licenses.add(createLicense());
    when(service.getLicensesForIdpAndSp(any(IdentityProvider.class), anyString())).thenReturn(licenses);
    List<Article> articles = new ArrayList<>();
    articles.add(createArticle());
    when(service.getArticlesForServiceProviders(anyListOf(String.class))).thenReturn(articles);

    cache = new CrmCache(service, dao, 0, 1000);
    //cache needs to kick in
    Thread.sleep(10);
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
    return new Article("lmngIdentifier");
  }

  private List<MappingEntry> getIdentityProviders() {
    return getProviders("idpId");
  }

  private List<MappingEntry> getServiceProviders() {
    return getProviders("spId");
  }

  private List<MappingEntry> getProviders(final String providerType) {
    List<MappingEntry> identityProviders = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      identityProviders.add(new MappingEntry(providerType + "-" + i, "lmngId-" + i));
    }
    return identityProviders;
  }


}
