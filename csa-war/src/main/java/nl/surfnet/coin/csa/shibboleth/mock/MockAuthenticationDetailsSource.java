package nl.surfnet.coin.csa.shibboleth.mock;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationDetailsSource;

import nl.surfnet.coin.csa.domain.CoinUser;

public class MockAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, CoinUser> {

  private final CoinUser coinUser;

  public MockAuthenticationDetailsSource() {
    coinUser = new CoinUser();
    coinUser.setDisplayName("csa_admin");
    coinUser.setEmail("csa-admin@surfnet.nl");
    coinUser.setUid("uid");
    coinUser.setInstitutionId("SURF");
  }

  @Override
  public CoinUser buildDetails(HttpServletRequest context) {
    return coinUser;
  }
}
