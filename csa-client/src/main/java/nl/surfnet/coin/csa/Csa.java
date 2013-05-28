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

import java.io.IOException;
import java.util.List;

import nl.surfnet.coin.csa.model.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface of CSA, the Cloud Services API.
 *
 */
public interface Csa {

  /**
   * Get a list of all services available to anyone.
   */
  List<Service> getPublicServices();

  /**
   * Get a list oof all protected services scoped by the Idp of the logged in person
   */
  List<Service> getProtectedServices();

    /**
     * Get a list of services, scoped by the given IDP entity ID
     */
  List<Service> getServicesForIdp(String idpEntityId);

  /**
   * Get a service by its CSA ID
   * @param id the ID
   */
  Service getService(long id);

  /**
   * Setter for base location of CSA
   * @param location base URL of CSA
   */
  void setCsaBaseLocation(String location);

  Taxonomy getTaxonomy() ;

  List<Action> getJiraActions();
}
