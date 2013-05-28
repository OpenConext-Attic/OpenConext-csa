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

import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import nl.surfnet.coin.csa.model.Category;
import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.csa.model.LicenseInformation;
import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.model.Taxonomy;

import javax.servlet.http.HttpServletRequest;

/**
 * Mock implementation of CSA. To be filled with lots of data for local development. Perhaps JSON-local-file-backed.
 */
public class CsaMock implements Csa {

  private List<Service> someServices() {
    return Arrays.asList(
      new Service(1L, "service 1", "http://logo-url", "http://website-url", false, null),
      new Service(2L, "service 2", "http://logo-url", "http://website-url", true, "foobar-crmlink"),
      new Service(3L, "service 3", "http://logo-url", "http://website-url", false, null),
      new Service(4L, "service 4", "http://logo-url", "http://website-url", false, null)
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
    return new Service(serviceId, "service " + serviceId, "http://123", "http://123231", false, null);
  }

  @Override
  public Service getService(long id) {
    return new Service(id, "service " + id, "http://123", "http://123231", false, null);
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
    return null;
  }
}
