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
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServicesCacheTest {

  private ServicesCache cache;
  private ServicesService service;

  @Test
  public void getServices() throws Exception {
    //the setup initializes the cache for the first hit
    List<Service> services = cache.getAllServices("en");
    assertEquals(1, services.size());

    Map<String, List<Service>> servicesMap = initServices();
    servicesMap.get("en").add(new Service());
    when(service.findAll(anyLong())).thenReturn(servicesMap);

    services = cache.getAllServices("en");
    assertEquals(1, services.size());

    //now wait for the cache to be updated
    Thread.sleep(1250);

    services = cache.getAllServices("en");
    assertEquals(2, services.size());
  }

  @Test
  public void serviceCacheShouldClone() {
    List<Service> services1 = cache.getAllServices("en");

    Service service1 = services1.get(0);
    List<Service> services2 = cache.getAllServices("en");
    Service service2 = services2.get(0);
    assertEquals("Cloned services should 'be equal'", service1, service2);
    assertFalse("Clones services should not be ==", service1 == service2);
  }

  private Map<String, List<Service>> initServices() {
    Map<String, List<Service>> services = new HashMap<String, List<Service>>();
    List<Service> nl = new ArrayList<Service>();
    Service service = new Service();
    nl.add(service);
    List<Service> en = new ArrayList<Service>();
    en.add(service);
    services.put("nl", nl);
    services.put("en", en);
    return services;
  }

  @Before
  public void setUp() throws Exception {
    cache = new ServicesCache();
    cache.setDelay(0);
    cache.setCallDelay(0L);
    cache.setDuration(1000);
    service = mock(ServicesService.class);
    cache.setService(service);

    Map<String, List<Service>> servicesMap = initServices();
    when(service.findAll(anyLong())).thenReturn(servicesMap);

    cache.afterPropertiesSet();

    //cache needs to kick in
    Thread.sleep(250);
  }


}
