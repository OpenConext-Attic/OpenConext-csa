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
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServicesCache extends AbstractCache {

  private static final Logger LOG = LoggerFactory.getLogger(ServicesCache.class);

  @Resource
  private ServicesService service;
  private ConcurrentHashMap<String, List<Service>> cache = new ConcurrentHashMap<String, List<Service>>();

  public List<Service> getAllServices(String lang) {
    Assert.isTrue("en".equalsIgnoreCase(lang) || "nl".equalsIgnoreCase(lang), "The only languages supported are 'nl' and 'en'");
    List<Service> services = cache.get(lang);
    if (services == null) {
      LOG.debug("Cache miss for lang '{}', will return empty list", lang);
      services = Collections.emptyList();
    }
    @SuppressWarnings("unchecked")
    List<Service> clone = (List<Service>) SerializationUtils.clone(new ArrayList<Service>(services));
    return clone;
  }

  public void setService(ServicesService service) {
    this.service = service;
  }

  @Override
  protected void doPopulateCache() {
    Map<String, List<Service>> services = service.findAll(callDelay);
    cache.clear();
    cache.putAll(services);
  }

  @Override
  protected String getCacheName() {
    return "Services Cache";
  }
}
