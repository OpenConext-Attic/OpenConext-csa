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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderCache implements InitializingBean {

  private static final Logger LOG = LoggerFactory.getLogger(ProviderCache.class);

  private @Value("${cacheMillisecondsStartupDelayTime}") long delay;

  private @Value("${cacheMillisecondsProviders}") long duration;

  private ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<String, List<String>>();

  @Resource
  private IdentityProviderService idpService;

  public List<String> getServiceProviderIdentifiers(String identityProviderId) {
    List<String> spIdentifiers = cache.get(identityProviderId);
    if (spIdentifiers == null) {
      spIdentifiers = idpService.getLinkedServiceProviderIDs(identityProviderId);
      cache.put(identityProviderId, spIdentifiers);
    }
    return spIdentifiers;
  }

  private void scheduleRefresh() {
    Timer timer = new Timer();

    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        LOG.info("Starting refreshing SR cache");
        try {
          Set<String> idpIdentifiers = cache.keySet();
          Map<String, List<String>> swap = new HashMap<String, List<String>>();
          for (String idpId : idpIdentifiers) {
            List<String> spIdentifiers = idpService.getLinkedServiceProviderIDs(idpId);
            swap.put(idpId, spIdentifiers);
          }
          cache.putAll(swap);
        } catch (Throwable t) {
          /*
           * anti pattern, but:
           *
           * http://stackoverflow.com/questions/637618/how-to-reschedule-a-task-using-a-scheduledexecutorservice
           * http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/ScheduledExecutorService.html#scheduleAtFixedRate(java.lang.Runnable,%20long,%20long,%20java.util.concurrent.TimeUnit)
           */
          LOG.error("Error in the refresh of the Providers cache", t);
        } finally {
          LOG.info("Finished refreshing SR cache");
        }
      }
    }, delay, duration);
  }


  @Override
  public void afterPropertiesSet() throws Exception {
    this.scheduleRefresh();
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void setIdpService(IdentityProviderService idpService) {
    this.idpService = idpService;
  }
}
