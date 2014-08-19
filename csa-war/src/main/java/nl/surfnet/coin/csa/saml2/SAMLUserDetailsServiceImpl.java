package nl.surfnet.coin.csa.saml2;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

import nl.surfnet.coin.csa.domain.CoinUser;

public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

  @Override
  public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
    return new CoinUser();
  }
}
