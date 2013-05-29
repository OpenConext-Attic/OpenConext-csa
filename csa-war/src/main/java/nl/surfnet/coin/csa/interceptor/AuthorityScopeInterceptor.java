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

import nl.surfnet.coin.csa.domain.CoinAuthority.Authority;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.util.SpringSecurity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.collection.LambdaCollections.with;
import static nl.surfnet.coin.csa.control.BaseController.*;
import static nl.surfnet.coin.csa.domain.CoinAuthority.Authority.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Interceptor to de-scope the visibility {@link CompoundServiceProvider}
 * objects for display
 * 
 * See <a
 * href="https://wiki.surfnetlabs.nl/display/services/App-omschrijving">https
 * ://wiki.surfnetlabs.nl/display/services/App-omschrijving</a>
 */
public class AuthorityScopeInterceptor extends HandlerInterceptorAdapter {

  public static final String OAUTH_CLIENT_SCOPE_JIRA = "csa";

  private static final Logger LOG = LoggerFactory.getLogger(AuthorityScopeInterceptor.class);

  private static List<String> TOKEN_CHECK_METHODS = Arrays.asList(new String[] { POST.name(), DELETE.name(), PUT.name() });

  private boolean exposeTokenCheckInCookie;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    //we don't want to token check csa api calls
    if (TOKEN_CHECK_METHODS.contains(request.getMethod().toUpperCase()) &&
            !request.getRequestURI().toLowerCase().contains("api/protected")) {
      String token = request.getParameter(TOKEN_CHECK);
      String sessionToken = (String) request.getSession().getAttribute(TOKEN_CHECK);
      if (StringUtils.isBlank(token) || !token.equals(sessionToken)) {
        throw new SecurityException(String.format("Token from session '%s' sdoes not match token '%s' from request", sessionToken, token));
      }
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
      throws Exception {

    if (modelAndView != null) {

      List<Authority> authorities = SpringSecurity.getCurrentUser().getAuthorityEnums();

      ModelMap map = modelAndView.getModelMap();

      scopeGeneralAuthCons(map, authorities, request);

      addTokenToModelMap(request, response,  map);
    }
  }

  protected void scopeGeneralAuthCons(ModelMap map, List<Authority> authorities, final HttpServletRequest request) {
    boolean isAdmin = containsRole(authorities, ROLE_DISTRIBUTION_CHANNEL_ADMIN);
    map.put(SERVICE_CONNECTION_VISIBLE, isAdmin);
    map.put(FACET_CONNECTION_VISIBLE, isAdmin);
    map.put(DEEPLINK_TO_SURFMARKET_ALLOWED, isAdmin);
    map.put(IS_GOD, isAdmin);
  }
  
  protected static boolean containsRole(List<Authority> authorities, Authority... authority) {
    for (Authority auth : authority) {
      if (authorities.contains(auth)) {
        return true;
      }
    }
    return false;
  }
  
  public static boolean isDistributionChannelAdmin() {
    return containsRole(SpringSecurity.getCurrentUser().getAuthorityEnums(), ROLE_DISTRIBUTION_CHANNEL_ADMIN);
  }

  public void setExposeTokenCheckInCookie(boolean exposeTokenCheckInCookie) {
    this.exposeTokenCheckInCookie = exposeTokenCheckInCookie;
  }

  /*
   * Add a security token to the modelMap that is rendered as hidden value in
   * POST forms. In the preHandle we check if the request is a POST and expect
   * equality of the token send as request parameter and the token stored in the
   * session
   */
  private void addTokenToModelMap(HttpServletRequest request, HttpServletResponse response, ModelMap map) {
    String token = UUID.randomUUID().toString();
    map.addAttribute(TOKEN_CHECK, token);
    request.getSession().setAttribute(TOKEN_CHECK, token);
    if (exposeTokenCheckInCookie) {
      response.addCookie(new Cookie(TOKEN_CHECK, token));
    }
  }

}
