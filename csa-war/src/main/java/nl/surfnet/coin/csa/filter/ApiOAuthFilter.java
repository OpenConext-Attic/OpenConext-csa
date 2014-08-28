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

package nl.surfnet.coin.csa.filter;

import static nl.surfnet.coin.csa.domain.CoinAuthority.Authority.ROLE_DISTRIBUTION_CHANNEL_ADMIN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;

import nl.surfnet.coin.api.client.InvalidTokenException;
import nl.surfnet.coin.api.client.OpenConextOAuthClient;
import nl.surfnet.coin.api.client.domain.Group20;
import nl.surfnet.coin.csa.domain.CoinAuthority;
import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.util.Constants;

/**
 * Servlet filter that performs Oauth 2.0 (authorization code) against
 * api.surfconext.nl and gets group information of the 'admin team'. Based on
 * this information, an additional role is set on the users' Authentication
 * object (or not).
 */
public class ApiOAuthFilter implements Filter {

  Logger LOG = LoggerFactory.getLogger(ApiOAuthFilter.class);

  private OpenConextOAuthClient apiClient;

  protected static final String PROCESSED = "nl.surfnet.coin.csa.filter.ApiOAuthFilter.PROCESSED";
  protected static final String ORIGINAL_REQUEST_URL = "nl.surfnet.coin.csa.filter.ApiOAuthFilter" + ".ORIGINAL_REQUEST_URL";
  private String adminDistributionTeam;
  private String callbackFlagParameter = "oauthCallback";

  @Autowired
  private Environment environment;


  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    if (!Arrays.asList(environment.getActiveProfiles()).contains(Constants.DEV_PROFILE_NAME)) {

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      final HttpSession session = httpRequest.getSession(true);

      if (isFullyAuthenticated() && session.getAttribute(PROCESSED) == null) {
        CoinUser user = (CoinUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (apiClient.isAccessTokenGranted(user.getUid())) {
          // already authorized before (we have a token)
          LOG.debug("Access token was already granted.");
          try {
            elevateUserIfApplicable(user);
            LOG.debug("Processed elevation. User is now: {}.", user);
            session.setAttribute(PROCESSED, "true");
          } catch (InvalidTokenException e) {
            initiateOauthAuthorization(httpRequest, httpResponse, session);
            return;
          } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
              LOG.error("Failed to check user membership elevation", e);
            } else {
              LOG.error("Failed to check user membership elevation", e.getMessage());
            }
          }
        } else if (httpRequest.getParameter(callbackFlagParameter) != null) {
          // callback from OAuth, elevate immediately afterwards

          apiClient.oauthCallback(httpRequest, user.getUid());

          try {
            elevateUserIfApplicable(user);
            session.setAttribute(PROCESSED, "true");
          } catch (InvalidTokenException e) {
            initiateOauthAuthorization(httpRequest, httpResponse, session);
            return;
          } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
              LOG.error("Failed to check user membership elevation", e);
            } else {
              LOG.error("Failed to check user membership elevation", e.getMessage());
            }
          }
          LOG.debug("Processed elevation after callback. Will redirect to originally requested location. User is now: {}.", user);
          ((HttpServletResponse) response).sendRedirect((String) session.getAttribute(ORIGINAL_REQUEST_URL));
          return;
        } else {
          // No authorization yet, start the OAuth dance
          LOG.debug("No auth yet, redirecting to auth url: {}", apiClient.getAuthorizationUrl());
          initiateOauthAuthorization(httpRequest, httpResponse, session);
          return;
        }
      }
    }

    chain.doFilter(request, response);
  }

  private void initiateOauthAuthorization(HttpServletRequest httpRequest, HttpServletResponse response, HttpSession session)
    throws IOException {
    final String currentRequestUrl = getCurrentRequestUrl(httpRequest);
    session.setAttribute(ORIGINAL_REQUEST_URL, currentRequestUrl);
    response.sendRedirect(apiClient.getAuthorizationUrl());
  }

  private String getCurrentRequestUrl(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder().append(request.getRequestURL());
    if (!StringUtils.isBlank(request.getQueryString())) {
      sb.append("?").append(request.getQueryString());
    }
    return sb.toString();
  }

  /**
   * Assign the appropriate roles to the given user, if he is member of one the
   * admin teams team.
   *
   * @param coinUser the CoinUser representing the currently logged in user.
   */
  private void elevateUserIfApplicable(CoinUser coinUser) {
    List<Group20> groups = apiClient.getGroups20(coinUser.getUid(), coinUser.getUid());

    if (LOG.isDebugEnabled()) {
      LOG.debug("Memberships of adminTeams '{}' for user '{}'", new Object[]{groups, coinUser.getUid()});
    }
    if (groupsContains(adminDistributionTeam, groups)) {
      coinUser.setAuthorities(new ArrayList<CoinAuthority>());
      coinUser.addAuthority(new CoinAuthority(ROLE_DISTRIBUTION_CHANNEL_ADMIN));
    }
  }

  private boolean groupsContains(String teamId, List<Group20> groups) {
    if (CollectionUtils.isEmpty(groups)) {
      return false;
    }
    for (Group20 group20 : groups) {
      if (group20.getId().equalsIgnoreCase(teamId)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isFullyAuthenticated() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CoinUser;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  public void setApiClient(OpenConextOAuthClient apiClient) {
    this.apiClient = apiClient;
  }

  public void setCallbackFlagParameter(String callbackFlagParameter) {
    this.callbackFlagParameter = callbackFlagParameter;
  }

  public void setAdminDistributionTeam(String adminDistributionTeam) {
    this.adminDistributionTeam = adminDistributionTeam;
  }

}
