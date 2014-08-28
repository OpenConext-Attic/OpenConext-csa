package nl.surfnet.coin.csa.shibboleth;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import com.google.common.base.Optional;

import nl.surfnet.coin.csa.util.Constants;

public class ShibbolethPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ShibbolethPreAuthenticatedProcessingFilter.class);

  @Autowired
  private Environment environment;

  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
    if (Arrays.asList(environment.getActiveProfiles()).contains(Constants.DEV_PROFILE_NAME)) {
      return new ShibbolethPrincipal("csa_admin", "dev admin", "admin@local", "http://mock-idp");
    }

    final Optional<String> uid = Optional.of((String) request.getAttribute(ShibbolethRequestAttributes.UID.getAttributeName()));
    if (uid.isPresent() ) {
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
