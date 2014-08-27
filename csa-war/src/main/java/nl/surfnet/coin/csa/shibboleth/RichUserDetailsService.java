package nl.surfnet.coin.csa.shibboleth;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import nl.surfnet.coin.csa.domain.CoinAuthority;
import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.util.Constants;

public class RichUserDetailsService implements AuthenticationUserDetailsService<Authentication> {

  @Autowired
  private Environment environment;

  @Override
  public CoinUser loadUserDetails(final Authentication token) throws UsernameNotFoundException {
    ShibbolethPrincipal richPrincipal = (ShibbolethPrincipal) token.getPrincipal();

    CoinUser coinUser = new CoinUser();
    coinUser.setUid(richPrincipal.getNameId());
    coinUser.setDisplayName(richPrincipal.getDisplayName());
    coinUser.setEmail(richPrincipal.getEmail());
    if (Arrays.asList(environment.getActiveProfiles()).contains(Constants.DEV_PROFILE_NAME)){
      setMockProperties(coinUser);
      return coinUser;
    }
    // TODO IDPs

    // TODO fetch groups to see if we have the admin role
    return coinUser;
  }

  private void setMockProperties(CoinUser coinUser) {
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
}
