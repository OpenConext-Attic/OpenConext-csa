/*
 * Copyright 2013 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.surfnet.coin.csa;

import nl.surfnet.coin.csa.model.*;
import nl.surfnet.coin.janus.domain.ARP;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Mock implementation of CSA. To be filled with lots of data for local development. Perhaps JSON-local-file-backed.
 */
public class CsaMock implements Csa {

  private List<Service> someServices() {
    return Arrays.asList(
            new Service(1L, "service 1", "http://logo-url", "http://website-url", false, "http://mock-sp", null),
            new Service(2L, "service 2", "http://logo-url", "http://website-url", true, "http://mock-sp", "foobar-crmlink"),
            new Service(3L, "service 3", "http://logo-url", "http://website-url", false, "http://mock-sp", null),
            new Service(4L, "service 4", "http://logo-url", "http://website-url", false, "http://mock-sp", null)
    );
  }

  @Override
  public List<Service> getPublicServices() {
    return someServices();
  }

  @Override
  public List<Service> getProtectedServices() {
    return Collections.emptyList();
  }

  @Override
  public List<Service> getServicesForIdp(String idpEntityId) {
    return someServices();
  }

  @Override
  public Service getServiceForIdp(String id, long serviceId) {
    Service service = new Service(serviceId, "service " + serviceId, "http://123", "http://123231", false, "http://mock-sp", null);
    service.setArp(new ARP());
    return service;
  }
  
  @Override
  public Service getServiceForIdp(String id, String spEntityId) {
    Service service = new Service(66L, "service " + 66L, "http://123", "http://123231", false, "http://mock-sp", null);
    service.setArp(new ARP());
    return service;
  }

  @Override
  public void setCsaBaseLocation(String location) {
  }

  @Override
  public Taxonomy getTaxonomy() {
    return new Taxonomy(Arrays.asList(
            new Category("cat1"),
            new Category("cat2"),
            new Category("cat3")
    ));
  }

  @Override
  public List<Action> getJiraActions() {
    Action action = new Action("TEST-123", "pietje.puk", "Pietje Puk", "pietje@puk.nl", JiraTask.Type.LINKREQUEST, JiraTask.Status.OPEN, "Body", "http://mock-idp",
            "http://mock-sp", "institutionId", new Date());
    return Arrays.asList(action);
  }

  @Override
  public Action createAction(Action action) {
    action.setJiraKey("TEST-" + System.currentTimeMillis());
    action.setStatus(JiraTask.Status.OPEN);
    return action;
  }
}
