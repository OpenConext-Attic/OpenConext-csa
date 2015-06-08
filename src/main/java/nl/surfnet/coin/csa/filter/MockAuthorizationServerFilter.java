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
package nl.surfnet.coin.csa.filter;

import nl.surfnet.coin.csa.domain.CheckTokenResponse;

import javax.servlet.*;
import java.io.IOException;
import java.util.Arrays;

import static nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor.*;

public class MockAuthorizationServerFilter extends AuthorizationServerFilter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    request.setAttribute(AuthorizationServerFilter.CHECK_TOKEN_RESPONSE, new CheckTokenResponse("http://mock-idp",Arrays.asList(OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES, OAUTH_CLIENT_SCOPE_ACTIONS, OAUTH_CLIENT_SCOPE_STATISTICS)));
    chain.doFilter(request, response);
  }
}
