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
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import nl.surfnet.coin.api.client.OpenConextOAuthClient;
import nl.surfnet.coin.api.client.domain.Group20;
import nl.surfnet.coin.csa.domain.CoinAuthority;
import nl.surfnet.coin.csa.domain.CoinAuthority.Authority;
import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.shibboleth.ShibbolethPrincipal;

@RunWith(MockitoJUnitRunner.class)
public class ApiOAuthFilterTest {

  private static final String THE_USERS_UID = "the-users-uid";

  @InjectMocks
  private ApiOAuthFilter filter = new ApiOAuthFilter();

  @Mock
  private FilterChain chain;

  @Mock
  private OpenConextOAuthClient apiClient;
  private static final Logger LOG = LoggerFactory.getLogger(ApiOAuthFilterTest.class);

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Environment environment;

  private CoinUser coinUser;

  @Before
  public void setUp() throws Exception {

    request = new MockHttpServletRequest("GET", "/anyUrl");
    response = new MockHttpServletResponse();
    String[] activeProfiles = {"foo", "bar"};
    when(environment.getActiveProfiles()).thenReturn( activeProfiles);
    SecurityContextHolder.setContext(securityContext);

  }

  @Test
  public void filterWhenNotLoggedInAtAll() throws Exception {
    Authentication mockAuthentication = mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(mockAuthentication);
    filter.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void filterWhenAlreadyProcessed() throws Exception {
    request.getSession().setAttribute(ApiOAuthFilter.PROCESSED, "true");
    setAuthentication();
    filter.doFilter(request, response, chain);

    // Filter should skip all logic and call chain.doFilter() straight on.
    verify(chain).doFilter(request, response);
    verifyNoMoreInteractions(apiClient);

  }

  @Test
  public void filterAndStartOauthDance() throws Exception {

    setAuthentication();

    when(apiClient.getAuthorizationUrl()).thenReturn("http://authorization-url");

    filter.doFilter(request, response, chain);
    LOG.debug("url: " + request.getRequestURL());
    assertThat("Originally requested url should be stored for later redirect (after oauth)",
      (String) request.getSession().getAttribute(ApiOAuthFilter.ORIGINAL_REQUEST_URL), IsEqual.equalTo("http://localhost:80/anyUrl"));
    assertThat("redirect to oauth authorization url", response.getRedirectedUrl(), IsEqual.equalTo("http://authorization-url"));
  }

  @Test
  public void filterAndProcessCallback() throws Exception {
    final HttpSession session = mock(HttpSession.class);
    setAuthentication();

    filter.setCallbackFlagParameter("myDummyCallback");
    filter.setAdminDistributionTeam("myAdminTeam");

    request.setParameter("myDummyCallback", "true");
    request.setSession(session);
    when(session.getAttribute(ApiOAuthFilter.ORIGINAL_REQUEST_URL)).thenReturn("http://originalUrl");
    filter.doFilter(request, response, chain);
    verify(apiClient).oauthCallback(eq(request), anyString());
    verify(apiClient).getGroups20(THE_USERS_UID, THE_USERS_UID);
    verify(session).setAttribute(ApiOAuthFilter.PROCESSED, "true");
    assertThat("redirect to original url", response.getRedirectedUrl(), IsEqual.equalTo("http://originalUrl"));
  }

  @Test
  public void filterAndUsePrefetchedAccessTokenButNoAdmin() throws Exception {
    when(apiClient.isAccessTokenGranted(anyString())).thenReturn(true);

    setAuthentication();

    filter.setAdminDistributionTeam("a-team");
    when(apiClient.getGroups20(THE_USERS_UID, THE_USERS_UID)).thenReturn(null);

    filter.doFilter(request, response, chain);
    assertThat((String) request.getSession().getAttribute(ApiOAuthFilter.PROCESSED), is("true"));
    assertNoRoleIsGranted(coinUser);
  }



  @Test
  public void filterAndUsePrefetchedAccessTokenAndIsAdmin() throws Exception {

    setAuthentication();
    coinUser.addAuthority(new CoinAuthority(Authority.ROLE_DISTRIBUTION_CHANNEL_ADMIN));
    when(apiClient.isAccessTokenGranted(anyString())).thenReturn(true);
    request.getSession(true).setAttribute(ApiOAuthFilter.PROCESSED, null);

    this.setUpGroupMembersShips(ROLE_DISTRIBUTION_CHANNEL_ADMIN);

    filter.doFilter(request, response, chain);

    assertRoleIsGranted(coinUser, ROLE_DISTRIBUTION_CHANNEL_ADMIN);
    // Verify flag that the process is done.
    assertThat((String) request.getSession().getAttribute(ApiOAuthFilter.PROCESSED), is("true"));

  }

  @Test
  public void test_elevate_user_results_in_only_one_csa_admin() throws IOException, ServletException {
    setUpForAuthoritiesCheck(ROLE_DISTRIBUTION_CHANNEL_ADMIN);
    assertRoleIsGranted(coinUser, ROLE_DISTRIBUTION_CHANNEL_ADMIN);
  }


  @Test
  public void test_elevate_user_results_in_no_authorities_in_lmng_disactive_modus() throws IOException, ServletException {
    setUpForAuthoritiesCheck(new Authority[]{});
    coinUser.setAuthorities(Collections.<CoinAuthority>emptyList());
    assertNoRoleIsGranted(coinUser);
  }

  private void setUpForAuthoritiesCheck(Authority... groupMemberShips) throws IOException, ServletException {
    request.getSession(true).setAttribute(ApiOAuthFilter.PROCESSED, null);
    when(apiClient.isAccessTokenGranted(anyString())).thenReturn(true);

    setAuthentication();

    setUpGroupMembersShips(groupMemberShips);

    filter.doFilter(request, response, chain);
    
  }

  protected static void assertNoRoleIsGranted(CoinUser user) {
    assertEquals(0, user.getAuthorityEnums().size());
  }

  protected static void assertRoleIsGranted(CoinUser user, CoinAuthority.Authority... expectedAuthorities) {
    List<CoinAuthority.Authority> actualAuthorities = user.getAuthorityEnums();
    assertEquals("expected roles: " + Arrays.asList(expectedAuthorities) + ", actual roles: " + actualAuthorities, expectedAuthorities.length, actualAuthorities.size());
    assertTrue("expected roles: " + Arrays.asList(expectedAuthorities) + ", actual roles: " + actualAuthorities, actualAuthorities.containsAll(Arrays.asList(expectedAuthorities)));
  }

  private void setUpGroupMembersShips(Authority... authorities) {
    List<Group20> groups = new ArrayList<>();
    for (Authority authority : authorities) {
      switch (authority) {
      case ROLE_DISTRIBUTION_CHANNEL_ADMIN:
        filter.setAdminDistributionTeam(authority.name());
        groups.add(new Group20(authority.name()));
        break;
      default:
      }
    }
    when(apiClient.getGroups20(THE_USERS_UID, THE_USERS_UID)).thenReturn(groups);
  }

  private void setAuthentication() {

    coinUser = new CoinUser();
    coinUser.setUid(THE_USERS_UID);
    final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(coinUser, "");
    token.setAuthenticated(true);
    when(securityContext.getAuthentication()).thenReturn(token);
  }

}
