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
import nl.surfnet.coin.janus.domain.JanusEntity;
import nl.surfnet.coin.oauth.ClientCredentialsClient;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CsaClientTestIntegration {

  private static String endpoint = "http://localhost:8282/csa";

  private static String answer = "{\"scope\":\"something\",\"access_token\":\"3fc6a956-a414-4f4b-a280-65cfbeb9ba2a\",\"token_type\":\"bearer\",\"expires_in\":0}";

  private static ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL).setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);

  /*
   * We need to mock the authorization server response for an client credentials access token
   */
  private static LocalTestServer oauth2AuthServer;

  protected static CsaClient csaClient;

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

  @Before
  public void before() throws Exception {
    setLanguage("en");
  }


  @Test
  public void taxonomy() throws IOException {
    doTestTaxonomy("Location");

    setLanguage("nl");

    doTestTaxonomy("Lokatie");
  }

  private void doTestTaxonomy(String facetName) {
    Taxonomy taxonomy = csaClient.getTaxonomy();
    List<Category> categories = taxonomy.getCategories();
    assertEquals(2, categories.size());
    assertEquals(2, categories.get(0).getValues().size());

    boolean facetNameFound = false;
    for (Category category : categories) {
      if (category.getName().equals(facetName)) {
        facetNameFound = true;
      }
      List<CategoryValue> values = category.getValues();
      for (CategoryValue value : values) {
        assertNotNull(value.getCategory());
      }
    }
    if (!facetNameFound) {
      fail();
    }
  }

  @Test
  public void publicServices() throws IOException {
    List<Service> publicServices = csaClient.getPublicServices();
    assertEquals(3, publicServices.size());
    for (Service service : publicServices) {
      assertNotNull(service);
    }
  }

  @Test
  public void getServiceBySpEntityID() {
    Service service = csaClient.getServiceForIdp("http://mock-idp", "http://mock-sp");
    assertNotNull(service);
    assertEquals("Populair SP (name en)", service.getName());
  }

  @Test
  public void getServiceBySpEntityIDranslated() {
    setLanguage("nl");
    Service service = csaClient.getServiceForIdp("http://mock-idp", "http://mock-sp");
    assertNotNull(service);
    assertEquals("Populaire SP (name nl)", service.getName());
  }

  private void setLanguage(final String language) {
  /*
   * Set up the Locale in the Request (as Spring does)
   */
    HttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new LocaleResolver() {
      @Override
      public Locale resolveLocale(HttpServletRequest request) {
        return new Locale(language);
      }
      @Override
      public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
      }
    });
    ServletRequestAttributes sra = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(sra);
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
  public void actionsForNonExistentProvider() throws IOException {
    List<Action> jiraActions = csaClient.getJiraActions("http://i-dont-exist");
    assertEquals(0, jiraActions.size());
  }

  @Test
  public void clearCache() throws IOException {
    csaClient.clearProviderCache();
    //not much we can assert, except that is still works
    List<InstitutionIdentityProvider> providers = csaClient.getAllInstitutionIdentityProviders();
    assertEquals(3, providers.size());
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
    assertEquals(2, providers.size());
    for (InstitutionIdentityProvider provider : providers) {
      assertEquals("mock-institution-id", provider.getInstitutionId());
    }
  }

  @Test
  public void allInstitutionsIdentityProviders() throws IOException {
    List<InstitutionIdentityProvider> providers = csaClient.getAllInstitutionIdentityProviders();
    assertEquals(3, providers.size());
    for (InstitutionIdentityProvider provider : providers) {
      assertNotNull(provider.getId());
    }
  }

  @Test
  public void serviceForIdp() throws IOException {
    Service service = csaClient.getServiceForIdp("http://mock-idp", 1);
    ARP arp = service.getArp();
    assertNotNull(arp);
    assertTrue(service.isConnected());
    assertNotNull(service.getCrmArticle());
    assertNotNull(service.getLicense());
  }

  @Test
  public void serviceForIdpNonExistent() throws IOException {
    try {
      csaClient.getServiceForIdp("http://mock-idp", Long.MAX_VALUE);
      fail();
    } catch (HttpClientErrorException e) {
      assertEquals(409, e.getStatusCode().value());
    }
  }

  @Test
  public void servicesByIdp() throws IOException {
    List<Service> services = csaClient.getServicesForIdp("http://mock-idp");
    assertEquals(5, services.size());
    //we need to check if 4 services are connected
    int connectedCount = 0;
    int crmLinked = 0;
    for (Service service : services) {
      assertNotNull(service);
      if (service.isConnected()) {
        ++connectedCount;
      }
      if (service.isHasCrmLink()) {
        ++crmLinked;
      }
    }
    assertEquals(3, connectedCount);
    assertEquals(4, crmLinked);
  }

  @Test
  public void servicesByIdpForNonExistent() {
    try {
      List<Service> services = csaClient.getServicesForIdp("http://i-don't-exist");
      fail();
    } catch (HttpClientErrorException e) {
      assertEquals(409, e.getStatusCode().value());
    }

  }

  @Test
  public void protectedServices() throws IOException {
    List<Service> protectedServices = csaClient.getProtectedServices();
    assertEquals(3, protectedServices.size());
    for (Service service : protectedServices) {
      assertNotNull(service);
    }
  }

  @Test
  public void testStatistics() {
    GregorianCalendar now = new GregorianCalendar();
    
    // before we create any actions we have
    Statistics before = csaClient.getStatistics(now.get(MONTH) + 1, now.get(YEAR));    
    assertNotNull(before);
    
    //create some actions
    Action action1 = new Action();
    action1.setBody("Body");
    action1.setSpId("http://mock-sp");
    action1.setIdpId("http://mock-idp");
    action1.setSubject("subject");
    action1.setType(JiraTask.Type.LINKREQUEST);
    action1.setUserEmail("test@integration.nl");
    action1.setUserId("urn.collab.test.integration");
    action1.setUserName("urn.collab.test.integration");
    action1.setInstitutionId("Institute1");
    csaClient.createAction(action1);
    
    Action action2 = new Action();
    action2.setBody("Body");
    action2.setSpId("http://mock-sp");
    action2.setIdpId("http://mock-idp");
    action2.setSubject("subject");
    action2.setType(JiraTask.Type.QUESTION);
    action2.setUserEmail("test@integration.nl");
    action2.setUserId("urn.collab.test.integration");
    action2.setUserName("urn.collab.test.integration");
    action2.setInstitutionId("Institute2");
    csaClient.createAction(action2);
    
    //after the action we have
    Statistics after = csaClient.getStatistics(now.get(MONTH) + 1, now.get(YEAR));
    assertNotNull(after);
    
    assertTrue("Number of link requests should increase", before.getTotalLinkRequests()+1 == after.getTotalLinkRequests());
    assertTrue("Number of unlink requests should stay the same", before.getTotalUnlinkRequests() == after.getTotalUnlinkRequests());
    assertTrue("Number of questions should increase", before.getTotalQuestions()+1 == after.getTotalQuestions());
    int linkRequestBefore = getCountFromInstituteMap(before.getInstitutionLinkRequests(), "Institute1");
    int questionBefore = getCountFromInstituteMap(before.getInstitutionQuestions(), "Institute2");
    assertEquals(new Integer(linkRequestBefore+1), after.getInstitutionLinkRequests().get("Institute1"));
    assertEquals(new Integer(questionBefore+1), after.getInstitutionQuestions().get("Institute2"));
  }
  
  private int getCountFromInstituteMap(Map<String, Integer> institutemap, String key) {
    int result = 0;
    if (null != institutemap.get(key)) {
      result = institutemap.get(key);
    }
    return result;
  }

}
