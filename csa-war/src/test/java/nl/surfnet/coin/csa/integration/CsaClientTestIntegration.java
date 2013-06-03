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
package nl.surfnet.coin.csa.integration;

import nl.surfnet.coin.csa.CsaClient;
import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.csa.model.JiraTask;
import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.model.Taxonomy;
import nl.surfnet.coin.janus.domain.ARP;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class CsaClientTestIntegration {

  private static String endpoint = "http://localhost:8280/csa";

  private static String answer = "{\"scope\":\"something\",\"access_token\":\"3fc6a956-a414-4f4b-a280-65cfbeb9ba2a\",\"token_type\":\"bearer\",\"expires_in\":0}";

  /*
   * We need to mock the authorization server response for an client credentials access token
   */
  private static LocalTestServer oauth2AuthServer;

  private static CsaClient csaClient;

  @BeforeClass
  public static void beforeClass() throws Exception {
    oauth2AuthServer = new LocalTestServer(null, null);
    oauth2AuthServer.start();
    oauth2AuthServer.register("/oauth2/token", new HttpRequestHandler() {
      @Override
      public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        response.setEntity(new StringEntity(answer, ContentType.APPLICATION_JSON));
        response.setStatusCode(200);
      }

    });
    String csaOAuth2AuthorizationUrl = String.format("http://%s:%d/oauth2/token", oauth2AuthServer.getServiceAddress().getHostName(),
            oauth2AuthServer.getServiceAddress().getPort());
    csaClient = new CsaClient(endpoint, csaOAuth2AuthorizationUrl, "key", "secret");
  }

  @Test
  public void taxonomy() throws IOException {
    Taxonomy taxonomy = csaClient.getTaxonomy();
    assertEquals(2, taxonomy.getCategories().size());

    assertEquals(2, taxonomy.getCategories().get(0).getValues().size());
  }

  @Test
  public void publicServices() throws IOException {
    List<Service> publicServices = csaClient.getPublicServices();
    assertEquals(58, publicServices.size());
    for (Service service : publicServices) {
      assertNotNull(service);
    }
  }

  @Test
  public void actions() throws IOException {
    List<Action> jiraActions = csaClient.getJiraActions();
    assertTrue(jiraActions.size() >= 3);
    for (Action action : jiraActions) {
      assertNotNull(action.getUserId());
    }
  }

  @Test
  public void newAction() throws Exception {
    Action action = new Action("jonh.doe", "john.doe@nl", "John Doe", JiraTask.Type.LINKREQUEST, "Body remarks", "http://mock-idp",
            "http://mock-sp", "mock-institution");
    action = csaClient.createAction(action);

    assertNotNull(action.getId());
    assertNotNull(action.getBody());
    assertNotNull(action.getJiraKey());
    assertNotNull(action.getRequestDate());
    assertEquals("mock-institution", action.getInstitutionId());
    assertEquals("http://mock-idp", action.getIdpId());
    assertEquals("john.doe@nl", action.getUserEmail());
    assertEquals(JiraTask.Status.OPEN, action.getStatus());
    assertEquals(JiraTask.Type.LINKREQUEST, action.getType());

    List<Action> jiraActions = csaClient.getJiraActions();
    action = jiraActions.get(jiraActions.size() - 1);

    assertEquals("John Doe", action.getUserName());


  }

  @Test
  public void serviceForIdp() throws IOException {
    Service service = csaClient.getServiceForIdp("http://mock-idp", 1);
    ARP arp = service.getArp();
    assertNotNull(arp);
  }

  @Test
  public void servicesByIdp() throws IOException {
    List<Service> services = csaClient.getServicesForIdp("http://mock-idp");
    assertEquals(29, services.size());
    for (Service service : services) {
      assertNotNull(service);
    }

    services = csaClient.getServicesForIdp("does-not-exist");
    assertEquals(0, services.size());
  }

  @Test
  public void protectedServices() throws IOException {
    List<Service> protectedServices = csaClient.getProtectedServices();
    assertEquals(29, protectedServices.size());
    for (Service service : protectedServices) {
      assertNotNull(service);
    }
  }


}
