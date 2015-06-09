package csa.shibboleth;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import csa.Application;


public class ShibbolethPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ShibbolethPreAuthenticatedProcessingFilter.class);

  private final Environment environment;

  public ShibbolethPreAuthenticatedProcessingFilter(AuthenticationManager authenticationManager, Environment environment) {
    super();
    setAuthenticationManager(authenticationManager);
    this.environment = environment;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
    LOG.debug("Attempting to find Principal for request {}", request.getRequestURI());
    if (environment.acceptsProfiles(Application.DEV_PROFILE_NAME)) {
      return new ShibbolethPrincipal("csa_admin", "dev admin", "admin@local", "http://mock-idp");
    }
    final Optional<String> uid = Optional.ofNullable(request.getHeader(ShibbolethRequestHeaders.UID.getHeaderName()));
    if (uid.isPresent()) {
      LOG.debug("Found user with uid {}", uid.get());
      final String displayName = request.getHeader(ShibbolethRequestHeaders.DISPLAY_NAME.getHeaderName());
      final String email = request.getHeader(ShibbolethRequestHeaders.EMAIL.getHeaderName());
      final String idpId = request.getHeader(ShibbolethRequestHeaders.IDP_ID.getHeaderName());
      return new ShibbolethPrincipal(uid.get(), displayName, email, idpId);
    } else {
      LOG.debug("No principal found. This should trigger shibboleth.");
      return null;
    }
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return "N/A";
  }
}
