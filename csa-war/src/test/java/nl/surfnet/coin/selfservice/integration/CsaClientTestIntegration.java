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
package nl.surfnet.coin.selfservice.integration;

import nl.surfnet.coin.csa.CsaClient;
import nl.surfnet.coin.csa.model.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class CsaClientTestIntegration {

  private String endpoint = "http://localhost:8280/selfservice";

  private CsaClient csaClient = new CsaClient(endpoint);

  @Test
  public void taxonomy() throws IOException {
    Taxonomy taxonomy = csaClient.getTaxonomy();
    assertEquals(2, taxonomy.getCategories().size());

    assertEquals(2, taxonomy.getCategories().get(0).getValues().size());
  }

  @Test
  public void publicServices() throws IOException {
    List<Service> publicServices = csaClient.getPublicServices();
    assertEquals(58,   publicServices.size());
    for (Service service : publicServices) {
      assertNotNull(service);
    }
  }

  @Test
  public void actions() throws IOException {
    List<Action> jiraActions = csaClient.getJiraActions();
    assertEquals(3,   jiraActions.size());
    for (Action action : jiraActions) {
      assertNotNull(action.getUserId());
    }
  }

  @Test
  public void servicesByIdp() throws IOException {
    List<Service> services = csaClient.getServicesForIdp("http://mock-idp");
    assertEquals(29,   services.size());
    for (Service service : services) {
      assertNotNull(service);
    }

    services = csaClient.getServicesForIdp("does-not-exist");
    assertEquals(0,   services.size());
  }

  @Test
  public void protectedServices() throws IOException {
    List<Service> protectedServices = csaClient.getProtectedServices();
    assertEquals(29,   protectedServices.size());
    for (Service service : protectedServices) {
      assertNotNull(service);
    }
  }

}
