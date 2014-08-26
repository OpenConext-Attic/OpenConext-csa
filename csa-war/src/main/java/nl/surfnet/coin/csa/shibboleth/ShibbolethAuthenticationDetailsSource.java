package nl.surfnet.coin.csa.shibboleth;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;

import nl.surfnet.coin.csa.domain.CoinUser;

public class ShibbolethAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, CoinUser> {

  private static Logger LOG = LoggerFactory.getLogger(ShibbolethAuthenticationDetailsSource.class);

  @Override
  public CoinUser buildDetails(final HttpServletRequest request) {
    String userId = request.getRemoteUser();
    String displayName = request.getHeader(ShibbolethConstants.DISPLAY_NAME);
    String email = request.getHeader(ShibbolethConstants.EMAIL);

    LOG.info("Found Shibboleth name-id: '{}', displayName: '{}', email: {}", userId, displayName, email);

    final CoinUser coinUser = new CoinUser();
    coinUser.setUid(userId);
    coinUser.setDisplayName(displayName);
    coinUser.setEmail(email);
    // TODO IDPs
    return coinUser;

  }
}
