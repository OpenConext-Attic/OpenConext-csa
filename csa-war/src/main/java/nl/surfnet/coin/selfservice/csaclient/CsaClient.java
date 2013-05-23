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

package nl.surfnet.coin.selfservice.csaclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.surfnet.coin.selfservice.api.model.LicenseInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for the CSA API. To be used by systems that need license information.
 */
@Component
public class CsaClient {

  private static final Logger LOG = LoggerFactory.getLogger(CsaClient.class);

  /**
   * Location of the API, no query parameters
   */
  @Value(value="${csa.location.licenses:not-defined-as-property}")
  private String csaLicensesLocation;

  RestTemplate tpl = new RestTemplate();

  public List<LicenseInformation> getLicenseInformation(String idpEntityId) {
    List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
    acceptableMediaTypes.add(MediaType.APPLICATION_JSON);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(acceptableMediaTypes);

    String locationWithParam = csaLicensesLocation + "?idpEntityId={idpEntityId}";

    LOG.debug("Will query CSA API with URL: {}", locationWithParam);
    Map variables = new HashMap<String, String>();
    variables.put("idpEntityId", idpEntityId);
    try {
      ResponseEntity<LicenseInformation[]> entity = tpl.getForEntity(locationWithParam, LicenseInformation[].class, variables);
      LicenseInformation[] licenseInformations = entity.getBody();
      if (licenseInformations != null) {
        LOG.debug("Got {} results from CSA API: {}", licenseInformations.length, licenseInformations);
        return Arrays.asList(licenseInformations);
      }
      LOG.info("No result from query to CSA, will return empty list.");
      return Collections.emptyList();
    } catch (Exception e) {
      LOG.error("Exception while using CSA API, will return empty list.", e);
      return Collections.emptyList();
    }
  }

  public void setCsaLicensesLocation(String csaLicensesLocation) {
    this.csaLicensesLocation = csaLicensesLocation;
  }

}
