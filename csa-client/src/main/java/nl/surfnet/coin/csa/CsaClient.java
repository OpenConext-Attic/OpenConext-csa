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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import nl.surfnet.coin.csa.model.Facet;
import nl.surfnet.coin.csa.model.LicenseInformation;
import nl.surfnet.coin.csa.model.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Client for the CSA API.
 */
@Named
public class CsaClient implements Csa {

  private static final Logger LOG = LoggerFactory.getLogger(CsaClient.class);

  /**
   * Location of the API
   */
  private String csaBaseLocation;

  RestTemplate tpl = new RestTemplate();
  private String csaEndpoint;

  @Override
  public List<Service> getPublicServices() {
    String url = "/public/services.json";
    return getFromCsa(url, null, Service[].class);
  }

  @Override
  public List<Service> getServicesForIdp(String idpEntityId) {
    String url = "/protected/services.json?idpEntityId={idpEntityId}";
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    return (List<Service>) getFromCsa(url, variables, Service[].class);
  }

  @Override
  public List<LicenseInformation> getLicenseInformation(String idpEntityId) {

    String locationWithParam = "/license/licenses.json?idpEntityId={idpEntityId}";

    LOG.debug("Will query CSA API with URL: {}", locationWithParam);
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    LicenseInformation[] licenseInformations  = (LicenseInformation[]) getFromCsa(locationWithParam, variables, LicenseInformation[].class);
    if (licenseInformations != null) {
      LOG.debug("Got {} results from CSA API: {}", licenseInformations.length, licenseInformations);
      return Arrays.asList(licenseInformations);
    }
    LOG.info("No result from query to CSA, will return empty list.");
    return Collections.emptyList();
  }

  @Override
  public List<Facet> getFacets() {
    String url = "/protected/facets.json";
    return (List<Facet>) getFromCsa(url, null, Facet[].class);
  }

  private<T> T getFromCsa(String url, Map<String, ?> variables, Class clazz) {
    try {
      ResponseEntity<T> entity = tpl.getForEntity(csaBaseLocation + url, clazz, variables);
      return entity.getBody();
    } catch (RuntimeException rte) {
      // TODO: differentiate between 4xx and 5xx?
      LOG.error("While using CSA API, will return null", rte);
      return null;
    }

  }

  public void setCsaBaseLocation(String csaBaseLocation) {
    this.csaBaseLocation = csaBaseLocation;
  }
}
