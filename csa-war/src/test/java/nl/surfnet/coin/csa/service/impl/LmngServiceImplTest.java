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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.surfnet.coin.csa.control.BaseController;
import nl.surfnet.coin.csa.dao.LmngIdentifierDao;

import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.service.CrmService;
import nl.surfnet.coin.shared.service.ErrorMessageMailer;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test class for {@code LmngServiceImpl}
 * 
 */
//@Configuration
//@EnableCaching
public class LmngServiceImplTest {//} implements HttpRequestHandler{

//  private static LocalTestServer testServer;
//  private static String endpoint;
//
//  private CrmService crmService;
//
//  @Mock
//  private LmngIdentifierDao lmngIdentifierDao;
//
//  @Mock
//  private JavaMailSender javaMailSender;
//
//  private String xmlFile;
//
//  @BeforeClass
//  public static void beforeClass() throws Exception {
//    testServer = new LocalTestServer(null, null);
//    testServer.start();
//
//    InetSocketAddress addr = testServer.getServiceAddress();
//    endpoint = "http://" + addr.getHostName() + "/mock/crm";
//
//  }
//
//  @Before
//  public void before() {
//    testServer.register("/mock/*", this);
//
//    MockitoAnnotations.initMocks(this);
//
//    ApplicationContext ctx = new AnnotationConfigApplicationContext(this.getClass());
//    crmService = ctx.getBean(CrmService.class);
//  }
//
//  @Override
//  public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
//    response.setEntity(new StringEntity(IOUtils.toString(new ClassPathResource(xmlFile).getInputStream())));
//    response.setStatusCode(200);
//  }
//
//  @Test
//  @Ignore
//  public void testCacheAnnotation() throws NoSuchMethodException, IOException {
//    IdentityProvider provider = new IdentityProvider();
//    provider.setId("https://mock-idp");
//
//    xmlFile = "lmng-xml/response_get_articles.xml";
//
//    when(lmngIdentifierDao.getLmngIdForServiceProviderId("https://mock-idp")).thenReturn("{12345678}");
//
//    List<Article> articles = crmService.getArticlesForServiceProviders(Collections.singletonList(provider.getId()));
//    assertEquals(1, articles.size());
//  }
//
//  @Bean
//  public CrmService crmService() {
//    LmngServiceImpl lmngService = new LmngServiceImpl();
//    lmngService.setEndpoint(endpoint);
//    lmngService.setLmngIdentifierDao(lmngIdentifierDao);
//    return lmngService;
//  }
//  @Bean
//  public CacheManager cacheManager() {
//    return new ConcurrentMapCacheManager();
//  }
//
//  @Bean
//  public ErrorMessageMailer errorMessageMailer() {
//    return new ErrorMessageMailer();
//  }
//
//  @Bean
//  public JavaMailSender javaMailSender() {
//    return javaMailSender;
//  }
//
//  @Bean
//  public LmngIdentifierDao lmngIdentifierDao() {
//    return lmngIdentifierDao;
//  }

}
