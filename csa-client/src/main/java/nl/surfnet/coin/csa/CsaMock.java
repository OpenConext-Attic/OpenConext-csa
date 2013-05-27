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

import java.util.List;

import nl.surfnet.coin.csa.model.Facet;
import nl.surfnet.coin.csa.model.LicenseInformation;
import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.model.Taxonomy;

/**
 * Mock implementation of CSA
 */
public class CsaMock implements Csa {
  @Override
  public List<Service> getPublicServices() {
    return null;
  }

  @Override
  public List<Service> getServicesForIdp(String idpEntityId) {
    return null;
  }

  @Override
  public List<LicenseInformation> getLicenseInformation(String idpEntityId) {
    return null;
  }

  @Override
  public Service getService(long id) {
    return null;
  }

  @Override
  public Taxonomy getTaxonomy() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
