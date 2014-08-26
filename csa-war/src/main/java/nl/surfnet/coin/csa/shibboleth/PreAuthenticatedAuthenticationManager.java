package nl.surfnet.coin.csa.shibboleth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class PreAuthenticatedAuthenticationManager implements AuthenticationManager {

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    authentication.setAuthenticated(true);
    return authentication;
  }
}
