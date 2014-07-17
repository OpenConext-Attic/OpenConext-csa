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
import nl.surfnet.coin.oauth.OauthClient;

import java.util.List;

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
   * Get a list of all protected services scoped by the Idp of the logged in person
   */
  List<Service> getProtectedServices();

    /**
     * Get a list of services, scoped by the given IDP entity ID
     */
  List<Service> getServicesForIdp(String idpEntityId);

  List<OfferedService> findOfferedServicesFor(String idpEntityId);

  /**
   * Get a service's details, scoped by the given IDP entity ID
   * @param idpEntityId
   * @param serviceId
   * @return
   */
  Service getServiceForIdp(String idpEntityId, long serviceId);
  
  /**
   * Get a service's details, scoped by the given IDP entity ID and SP entity ID
   * @param idpEntityId idp entity ID
   * @param spEntityId sp entity ID
   * @return
   */
  Service getServiceForIdp(String idpEntityId, String spEntityId);

  /**
   * Setter for base location of CSA
   * @param location base URL of CSA
   */
  void setCsaBaseLocation(String location);
  
  /**
   * Retrieve statistical information from CSA for the given month and year. Statistical information
   * consists of the number of questions, link- and unlinkrequests per IDP/SP.
   * @param month month to retrieve information for (1-12)
   * @param year the year to retrieve informaiton for
   * @return the statistics
   * @see Statistics
   */
  Statistics getStatistics(final int month, final int year);

  Taxonomy getTaxonomy() ;

  List<Action> getJiraActions(String idpEntityId);

  void clearProviderCache();

  Action createAction(Action action);

  List<InstitutionIdentityProvider> getInstitutionIdentityProviders(String identityProviderId);

  List<InstitutionIdentityProvider> getAllInstitutionIdentityProviders();

  void setOauthClient(OauthClient client);
}
