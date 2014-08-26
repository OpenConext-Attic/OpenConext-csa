package nl.surfnet.coin.csa.shibboleth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

public class ShibbolethPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {


  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
    return request.getRemoteUser();
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return "N/A";
  }
}
