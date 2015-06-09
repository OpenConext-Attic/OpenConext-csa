package csa.shibboleth;

import java.util.Arrays;
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
    if (Arrays.asList(environment.getActiveProfiles()).contains(Application.DEV_PROFILE_NAME)) {
      return new ShibbolethPrincipal("csa_admin", "dev admin", "admin@local", "http://mock-idp");
    }

    final Optional<String> uid = Optional.ofNullable((String) request.getAttribute(ShibbolethRequestAttributes.UID.getAttributeName()));
    if (uid.isPresent()) {
      LOG.debug("Found user with uid {}", uid.get());
      final String displayName = (String) request.getAttribute(ShibbolethRequestAttributes.DISPLAY_NAME.getAttributeName());
      final String email = (String) request.getAttribute(ShibbolethRequestAttributes.EMAIL.getAttributeName());
      final String idpId = (String) request.getAttribute(ShibbolethRequestAttributes.IDP_ID.getAttributeName());
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
