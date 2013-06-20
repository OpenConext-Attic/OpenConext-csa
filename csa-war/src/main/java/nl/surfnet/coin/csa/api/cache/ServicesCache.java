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

import nl.surfnet.coin.csa.api.control.ServicesService;
import nl.surfnet.coin.csa.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class ServicesCache implements InitializingBean {

  private static final Logger LOG = LoggerFactory.getLogger(ServicesCache.class);

  @Resource
  private ServicesService service;

  private @Value("${cacheMillisecondsServices}") long duration;

  private @Value("${cacheMillisecondsStartupDelayTime}") long delay;

  private ConcurrentHashMap<String, List<Service>> cache = new ConcurrentHashMap<String, List<Service>>();

  private void scheduleRefresh() {
    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        LOG.info("Starting refreshing Services cache");
        try {
          Map<String, List<Service>> services = service.findAll();
          cache.putAll(services);
        } catch (Throwable t) {
          /*
           * anti pattern, but:
           *
           * http://stackoverflow.com/questions/637618/how-to-reschedule-a-task-using-a-scheduledexecutorservice
           * http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/ScheduledExecutorService.html#scheduleAtFixedRate(java.lang.Runnable,%20long,%20long,%20java.util.concurrent.TimeUnit)
           */
          LOG.error("Error in the refresh of the Services cache", t);
        }
        finally {
          LOG.info("Finished refreshing Services cache");
        }
      }
    }, 2500, duration);
  }

  public List<Service> getAllServices(String lang) {
    Assert.isTrue("en".equalsIgnoreCase(lang) || "nl".equalsIgnoreCase(lang), "The only languages supported are 'nl' and 'en'");
    return cache.get(lang);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    scheduleRefresh();
  }

  public void setService(ServicesService service) {
    this.service = service;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }
}
