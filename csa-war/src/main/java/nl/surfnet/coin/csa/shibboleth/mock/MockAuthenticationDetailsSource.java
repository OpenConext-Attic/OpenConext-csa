package nl.surfnet.coin.csa.shibboleth.mock;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationDetailsSource;

import nl.surfnet.coin.csa.domain.CoinAuthority;
import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.domain.IdentityProvider;

public class MockAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, CoinUser> {

  private final CoinUser coinUser;

  public MockAuthenticationDetailsSource() {
    coinUser = new CoinUser();
    coinUser.setDisplayName("csa_admin");
    coinUser.setEmail("csa-admin@surfnet.nl");
    coinUser.setUid("uid");
    final String institutionId = "mock-institution-id";
    coinUser.setInstitutionId(institutionId);

    coinUser.setAuthorities(Arrays.asList(new CoinAuthority(CoinAuthority.Authority.ROLE_DISTRIBUTION_CHANNEL_ADMIN)));
    final String idpId = "http://mock-idp";
    final IdentityProvider currentIdp = new IdentityProvider(idpId, institutionId, "Main IdP (name en)");
    final IdentityProvider otherIdp = new IdentityProvider("https://idp_with_all_but_one_sp", institutionId, "IdP with All SP's connected, but one (name en)");

    coinUser.addInstitutionIdp(currentIdp);
    coinUser.addInstitutionIdp(otherIdp);




    coinUser.setIdp(currentIdp);
  }

  @Override
  public CoinUser buildDetails(HttpServletRequest context) {
    return coinUser;
  }
}
