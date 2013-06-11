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
import nl.surfnet.coin.csa.model.*;
import nl.surfnet.coin.janus.domain.ARP;
import nl.surfnet.coin.shared.oauth.ClientCredentialsClient;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.*;

public class CsaClientTestIntegration {

  private static String endpoint = "http://localhost:8282/csa";

  private static String answer = "{\"scope\":\"something\",\"access_token\":\"3fc6a956-a414-4f4b-a280-65cfbeb9ba2a\",\"token_type\":\"bearer\",\"expires_in\":0}";

  private static ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                                            .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL).setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);

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
    csaClient = new CsaClient(endpoint);
    ClientCredentialsClient oauthClient = new ClientCredentialsClient();
    oauthClient.setClientKey("key");
    oauthClient.setClientSecret("secret");
    oauthClient.setOauthAuthorizationUrl(csaOAuth2AuthorizationUrl);
    csaClient.setOauthClient(oauthClient);
  }

  @Test
  public void taxonomy() throws IOException {
    Taxonomy taxonomy = csaClient.getTaxonomy();
    List<Category> categories = taxonomy.getCategories();
    assertEquals(2, categories.size());
    assertEquals(2, categories.get(0).getValues().size());

    for (Category category : categories) {
      List<CategoryValue> values = category.getValues();
      for (CategoryValue value : values) {
        assertNotNull(value.getCategory());
      }
    }
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
    List<Action> jiraActions = csaClient.getJiraActions("http://mock-idp");
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

    List<Action> jiraActions = csaClient.getJiraActions("http://mock-idp");
    action = jiraActions.get(jiraActions.size() - 1);

    assertEquals("John Doe", action.getUserName());
  }

  @Test
  public void institutionsIdentityProviders() throws IOException {
    List<InstitutionIdentityProvider> providers = csaClient.getInstitutionIdentityProviders("http://mock-idp");
    assertEquals(3, providers.size());
    for (InstitutionIdentityProvider provider : providers) {
      assertEquals("mock-institution-id", provider.getInstitutionId());
    }
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
