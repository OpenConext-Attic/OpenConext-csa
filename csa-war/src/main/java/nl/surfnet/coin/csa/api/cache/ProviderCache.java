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

import nl.surfnet.coin.csa.service.IdentityProviderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderCache extends AbstractCache {

  private ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<String, List<String>>();

  @Resource
  private IdentityProviderService idpService;

  public List<String> getServiceProviderIdentifiers(String identityProviderId) {
    List<String> spIdentifiers = cache.get(identityProviderId);
    if (spIdentifiers == null) {
      spIdentifiers = idpService.getLinkedServiceProviderIDs(identityProviderId);
      cache.put(identityProviderId, spIdentifiers);
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
    Set<String> idpIdentifiers = cache.keySet();
    Map<String, List<String>> swap = new HashMap<String, List<String>>();
    for (String idpId : idpIdentifiers) {
      List<String> spIdentifiers = idpService.getLinkedServiceProviderIDs(idpId);
      swap.put(idpId, spIdentifiers);
    }
    cache.putAll(swap);

  }

  @Override
  protected String getCacheName() {
    return "Service Registry Cache";
  }
}
