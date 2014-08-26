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
package nl.surfnet.coin.csa.interceptor;

import static nl.surfnet.coin.csa.control.BaseController.TOKEN_CHECK;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.CoinAuthority;
import nl.surfnet.coin.csa.domain.CoinAuthority.Authority;
import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.ContactPerson;
import nl.surfnet.coin.csa.domain.ContactPersonType;
import nl.surfnet.coin.csa.domain.ServiceProvider;

public class AuthorityScopeInterceptorTest {

  private AuthorityScopeInterceptor interceptor = new AuthorityScopeInterceptor();

  @Test
  public void token_session_does_not_equal_request_param_token() throws Exception {
    ModelAndView modelAndView = new ModelAndView();
    CoinUser user = coinUser(Authority.ROLE_DISTRIBUTION_CHANNEL_ADMIN);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getDetails()).thenReturn(user);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    MockHttpServletRequest request = new MockHttpServletRequest();
    interceptor.postHandle(request, null, null, modelAndView);

    // first check if the token is generated and stored in session and modelMap
    String token = (String) modelAndView.getModelMap().get(TOKEN_CHECK);
    assertNotNull(token);

    String sessionToken = (String) request.getSession(false).getAttribute(TOKEN_CHECK);
    assertNotNull(token);
    assertEquals(token, sessionToken);

    // now check if the prehandle checks the token if the method is a POST
    request = new MockHttpServletRequest();
    request.setMethod(RequestMethod.POST.name());
    try {
      interceptor.preHandle(request, null, null);
      fail("Expected security exception");
    } catch (Exception e) {
    }

    // now check if the prehandle checks the token if the method is a POST
    request = new MockHttpServletRequest();
    request.addParameter(TOKEN_CHECK, sessionToken);
    request.getSession().setAttribute(TOKEN_CHECK, sessionToken);
    request.setMethod(RequestMethod.POST.name());

    assertTrue(interceptor.preHandle(request, null, null));

  }

  private CoinUser coinUser(Authority... authorities) {
    CoinUser coinUser = new CoinUser();
    for (Authority authority : authorities) {
      coinUser.addAuthority(new CoinAuthority(authority));
    }
    return coinUser;
  }


}
