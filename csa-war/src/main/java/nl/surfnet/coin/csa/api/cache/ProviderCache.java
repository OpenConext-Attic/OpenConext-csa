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

import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.service.IdentityProviderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderCache extends AbstractCache {

  /**
   * This is a lazy-loading cache. Initially, the populateCache() will not do any call to a backend.
   * Each call to getServiceProvider() first checks the cache. If not found, load lazily and put in cache.
   * Subsequent populateCache()'s will then only update this already loaded items.
   */
  private ConcurrentHashMap<String, List<String>> spIdsCache = new ConcurrentHashMap<String, List<String>>();


  private ConcurrentHashMap<String, IdentityProvider> idpCache = new ConcurrentHashMap<String, IdentityProvider>();

  @Resource
  private IdentityProviderService idpService;

  public List<String> getServiceProviderIdentifiers(String identityProviderId) {

    List<String> spIdentifiers = spIdsCache.get(identityProviderId);
    if (spIdentifiers == null) {
      spIdentifiers = idpService.getLinkedServiceProviderIDs(identityProviderId);
      spIdsCache.put(identityProviderId, spIdentifiers);
    }
    if (spIdentifiers == null) {
      spIdentifiers = Collections.emptyList();
    }
    return spIdentifiers;
  }

  public void setIdpService(IdentityProviderService idpService) {
    this.idpService = idpService;
  }

  @Override
  protected void doPopulateCache() {
    populateSPIds();
    populateIdps();

  }

  private void populateIdps() {
    List<IdentityProvider> allIdentityProviders = idpService.getAllIdentityProviders();
    ConcurrentHashMap<String, IdentityProvider> newIdpCache = new ConcurrentHashMap<String, IdentityProvider>(allIdentityProviders.size());
    for (IdentityProvider idp : allIdentityProviders) {
      newIdpCache.put(idp.getId(), idp);
    }
    idpCache = newIdpCache;
  }

  public IdentityProvider getIdentityProvider(String idpEntityId) {
    IdentityProvider identityProvider = idpCache.get(idpEntityId);
    //kind of bizar, means we have a new IdP in between cache re-populate (happens in theory only and in integration tests)
    if (identityProvider == null) {
      identityProvider = idpService.getIdentityProvider(idpEntityId);
      if (identityProvider != null) {
        idpCache.put(identityProvider.getId(), identityProvider);
      }
    }
    return identityProvider;
  }

  private void populateSPIds() {
    Set<String> idpIdentifiers = spIdsCache.keySet();
    Map<String, List<String>> swap = new HashMap<String, List<String>>();
    for (String idpId : idpIdentifiers) {
      List<String> spIdentifiers = idpService.getLinkedServiceProviderIDs(idpId);
      swap.put(idpId, spIdentifiers);
    }
    spIdsCache.putAll(swap);
  }

  @Override
  protected String getCacheName() {
    return "Service Registry Cache";
  }
}
