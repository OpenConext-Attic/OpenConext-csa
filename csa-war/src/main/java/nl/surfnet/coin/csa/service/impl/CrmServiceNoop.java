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
package nl.surfnet.coin.csa.service.impl;

import nl.surfnet.coin.csa.domain.Account;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.model.License;
import nl.surfnet.coin.csa.service.CrmService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * No operation CrmService to be used in OpenConext
 */
public class CrmServiceNoop implements CrmService{

  private boolean debug;

  private String endpoint;

  @Override
  public List<License> getLicensesForIdpAndSp(IdentityProvider identityProvider, String articleIdentifier, Date validOn) throws LmngException {
    return new ArrayList<License>();
  }

  @Override
  public List<License> getLicensesForIdpAndSps(IdentityProvider identityProvider, List<String> articleIdentifiers, Date validOn) throws LmngException {
    return new ArrayList<License>();
  }

  @Override
  public List<Article> getArticlesForServiceProviders(List<String> serviceProviderEntityIds) throws LmngException {
    return new ArrayList<Article>();
  }

  @Override
  public String getInstitutionName(String guid) {
    return null;
  }

  @Override
  public Article getService(String guid) {
    return Article.NONE;
  }

  @Override
  public String getServiceName(String lmngId) {
    return null;
  }

  @Override
  public List<Account> getAccounts(boolean isInstitution) {
    return new ArrayList<Account>();
  }

  @Override
  public String performQuery(String rawQuery) {
    return null;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }


}
