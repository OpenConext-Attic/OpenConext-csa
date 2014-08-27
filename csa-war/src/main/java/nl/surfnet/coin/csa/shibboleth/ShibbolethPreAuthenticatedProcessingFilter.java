package nl.surfnet.coin.csa.shibboleth;

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import nl.surfnet.coin.csa.util.Constants;

public class ShibbolethPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {



  @Autowired
  private Environment environment;

  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
    if (Arrays.asList(environment.getActiveProfiles()).contains(Constants.DEV_PROFILE_NAME)) {
      return new ShibbolethPrincipal("csa_admin", "dev admin", "admin@local");
    }

    final String nameId = request.getRemoteUser();
    if (nameId != null ) {
      // TODO hans fetch the display-name and the email from the request somewhere
      return new ShibbolethPrincipal(nameId, "some admin fix this", "foo@fix.this");
    } else {
      return null;
    }
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return "N/A";
  }
}
