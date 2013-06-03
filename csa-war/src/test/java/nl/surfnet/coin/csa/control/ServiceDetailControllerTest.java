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

package nl.surfnet.coin.csa.control;

import javax.servlet.http.HttpServletRequest;

import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.domain.OAuthTokenInfo;
import nl.surfnet.coin.csa.service.impl.CompoundSPService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link nl.surfnet.coin.csa.control.ServiceDetailController}
 */
public class ServiceDetailControllerTest {

  @InjectMocks
  private ServiceDetailController controller;

  @Mock
  private CoinUser coinUser;

  @Mock
  private CompoundSPService compoundSPService;
  
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    controller = new ServiceDetailController();
    MockitoAnnotations.initMocks(this);
    
    request = new MockHttpServletRequest();
    when(coinUser.getUid()).thenReturn("urn:collab:person:example.edu:john.doe");
    SecurityContextHolder.getContext().setAuthentication(getAuthentication());
  }

  @Test
  public void testSpDetail() throws Exception {

    IdentityProvider idp = new IdentityProvider();
    idp.setId("mockIdP");
    CompoundServiceProvider csp = new CompoundServiceProvider();
    when(compoundSPService.getCSPByServiceProviderEntityId("serviceProviderEntityId")).thenReturn(csp);

    OAuthTokenInfo info = new OAuthTokenInfo("cafebabe-cafe-babe-cafe-babe-cafebabe", "mockDao");
    info.setUserId(coinUser.getUid());

    final ModelAndView modelAndView = controller.serviceDetail("serviceProviderEntityId", request);
    assertEquals("app-detail", modelAndView.getViewName());
    assertEquals(csp, modelAndView.getModelMap().get("compoundSp"));
    assertNull(modelAndView.getModelMap().get("revoked"));
  }

  private Authentication getAuthentication() {
    return new TestingAuthenticationToken(coinUser, "");
  }
  
}
