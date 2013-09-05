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
package nl.surfnet.coin.csa.util;

import static nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_ACTIONS;
import static nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES;
import static nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_STATISTICS;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.surfnet.oaaas.auth.AuthorizationServerFilter;
import org.surfnet.oaaas.auth.principal.AuthenticatedPrincipal;
import org.surfnet.oaaas.model.VerifyTokenResponse;

public class MockAuthorizationServerFilter extends AuthorizationServerFilter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    AuthenticatedPrincipal principal = new AuthenticatedPrincipal("john.doe", Arrays.asList("user"), new HashMap<String, String>(), Arrays.asList("csa_shopmanager"), false);
    principal.addAttribute("IDENTITY_PROVIDER",  "http://mock-idp");
    principal.addAttribute("DISPLAY_NAME",  "John Doe");
    VerifyTokenResponse tokenResponse = new VerifyTokenResponse(
            "client-name-mocked",
            Arrays.asList(OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES, OAUTH_CLIENT_SCOPE_ACTIONS, OAUTH_CLIENT_SCOPE_STATISTICS),
            principal, null);
    request.setAttribute(AuthorizationServerFilter.VERIFY_TOKEN_RESPONSE, tokenResponse);
    chain.doFilter(request, response);
  }
}
