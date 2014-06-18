/*
 * Copyright 2012 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.surfnet.coin.csa.service.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import nl.surfnet.coin.csa.dao.LmngIdentifierDao;
import nl.surfnet.coin.csa.domain.Account;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.model.License;
import nl.surfnet.coin.csa.service.CrmService;
import nl.surfnet.coin.shared.service.ErrorMessageMailer;

/**
 * LmngServiceImplIT.java
 * NOTE! we us this for a local integration test only
 */
@RunWith(MockitoJUnitRunner.class)
public class LmngServiceImplIT {

  private static Logger LOG = LoggerFactory.getLogger(LmngServiceImpl.class);

  @InjectMocks
  private LmngServiceImpl subject = new LmngServiceImpl();

  @Mock
  private LmngIdentifierDao lmngIdentifierDao;

  @Mock
  private ErrorMessageMailer errorMessageMailer;

  private static String ENDPOINT = "https://crmproxypilot.surfmarket.nl/crmservice.svc";

  @Before
  public void before() {
    subject.setEndpoint(ENDPOINT);
    subject.setDebug(true);

  }

  //   we us this for a local integration test only
//  @Test
  public void testRetrieveLmngGoogleEdugroepGreencloudSurfMarket() throws IOException, LmngException {

    List<String> spIds = new ArrayList<>();
    spIds.add("http://www.google.com");
    spIds.add("Greencloud");
    spIds.add("EDUgroepen");

    List<Article> articles = subject.getArticlesForServiceProviders(spIds);

    assertNotNull(articles);
    assertEquals("Incorrect number of results", 3, articles.size());

    assertEquals("Incorrect name for product", "GreenQloud", articles.get(0).getProductName());

    assertEquals("Incorrect name for product", "Google apps voor edu", articles.get(1).getProductName());

    assertEquals("Incorrect name for product", "Edugroepen", articles.get(2).getProductName());

  }

//  @Test
  public void testRetrievalAllAccounts() throws IOException {
    List<Account> accounts = subject.getAccounts(true);
    LOG.debug("Num accounts: {}", accounts.size());

    accounts = subject.getAccounts(false);
    LOG.debug("Num accounts: {}", accounts.size());
    ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

  }

//  @Test
  public void testPerformArticles() throws Exception {
    String query = IOUtils.toString(new ClassPathResource("lmngqueries/lmngQueryAllArticles.xml").getInputStream());
    String result = subject.performQuery(query);
    System.out.println(StringEscapeUtils.unescapeHtml(result));
  }

  // we us this for a local integration test only
//  @Test
  public void testRetrieveInstitutionName() throws IOException {
    String guid = "{ED3207DC-1910-DC11-A6C7-0019B9DE3AA4}";

    String instituteName = subject.getInstitutionName(guid);

    assertNotNull(instituteName);
    assertEquals("Incorrect institution name", "Open Universiteit Nederland", instituteName);
  }

  // we us this for a local integration test only
//  @Test
  public void testRetrieveArticle() throws IOException {
    String guid =
            "{A1EA4AF9-6C9E-E111-B429-0050569E0013}";
    Article instituteName = subject.getService(guid);

    assertNotNull(instituteName);
  }

//  @Test
  public void testRetrieveAcademiaLicenseForErasmus() throws LmngException {
    IdentityProvider identityProvider = new IdentityProvider();
    identityProvider.setId("erasmus");
    identityProvider.setInstitutionId("Erasmus");
    final String erasmuslLmngId = "{1F73865F-900F-DC11-A6C7-0019B9DE3AA4}";
    final String academiaArticleId = "{B6B32EB5-4091-E211-9DB6-0050569E0011}";
    when(this.lmngIdentifierDao.getLmngIdForIdentityProviderId(identityProvider.getInstitutionId())).thenReturn(erasmuslLmngId);

    // {F46CCB08-6135-E111-B32A-0050569E0007} {4EF1EE04-ED7C-E111-8393-0050569E0011} {FFA274E1-E5DA-E111-8363-0050569E0011} {6157077A-D933-E211-BCF7-0050569E0013}

    List<License> result = subject.getLicensesForIdpAndSp(identityProvider, academiaArticleId);
    assertTrue(result.size() == 0);
  }


//  @Test
  public void testRawQuery() {
    subject.performQuery("<fetch version=\"1.0\" output-format=\"xml-platform\" mapping=\"logical\" distinct=\"true\">" +
      "<entity name=\"lmng_sdnarticle\">" +
//"   <filter>"+
//"     <condition attribute=\"lmng_sdnarticleid\" operator=\"in\">"+
//"       <value>{099F8003-64A7-E211-9388-0050569E66E5}</value>"+
//"     </condition>"+
//"   </filter>"+
      "   <link-entity name=\"lmng_sdnarticle_lmng_product\" from=\"lmng_sdnarticleid\" to=\"lmng_sdnarticleid\" visible=\"false\" intersect=\"true\">" +
      "         <attribute name=\"lmng_sdnarticleid\"/>" +
      "     <link-entity name=\"lmng_product\" from=\"lmng_productid\" to=\"lmng_productid\" alias=\"product\">" +
      "        <link-entity name=\"lmng_productvariation\" from=\"lmng_productid\" to=\"lmng_productid\" alias=\"productvariation\">" +
      "         <attribute name=\"lmng_licensemodel\"/>" +
      "         <link-entity name=\"lmng_licenseagreement\" from=\"lmng_productvariationid\" to=\"lmng_productvariationid\" alias=\"license\" >" +
      "           <attribute name=\"lmng_number\"/>" +
      "           <attribute name=\"lmng_validfrom\"/>" +
      "           <attribute name=\"lmng_validto\"/>" +
      "           <attribute name=\"lmng_organisationid\"/>" +
      "           <filter type=\"and\">" +
      "             <condition attribute=\"lmng_validfrom\" operator=\"on-or-before\" value=\"2013-05-01\" />" +
      "             <condition attribute=\"lmng_validto\" operator=\"on-or-after\" value=\"2013-05-01\" />" +
      "             <condition attribute=\"statuscode\" operator=\"eq\" value=\"4\"/>" +
      "             <condition attribute=\"lmng_organisationid\" operator=\"eq\" value=\"{837326CA-1A10-DC11-A6C7-0019B9DE3AA4}\" />" +
      "           </filter>" +
      "         </link-entity>" +
      "       </link-entity>" +
      "     </link-entity>" +
      "   </link-entity>" +
      " </entity>" +
      "</fetch>");
  }

}
