package nl.surfnet.coin.csa.saml2;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.util.Assert;

import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.service.IdentityProviderService;
import nl.surfnet.coin.janus.Janus;
import nl.surfnet.coin.janus.domain.JanusEntity;

public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

  private static final Logger LOG = LoggerFactory.getLogger(SAMLUserDetailsServiceImpl.class);

  private static final String DISPLAY_NAME_ATTTRIBUTE_NAME = "urn:mace:dir:attribute-def:displayName";
  private static final String EMAIL_ATTTRIBUTE_NAME = "urn:mace:dir:attribute-def:mail";
  private static final String SCHAC_HOME_ATTTRIBUTE_NAME = "urn:mace:terena.org:attribute-def:schacHomeOrganization";
  private static final String UUID_ATTTRIBUTE_NAME = "urn:oid:1.3.6.1.4.1.1076.20.40.40.1";

  @Autowired
  private IdentityProviderService identityProviderService;

  @Autowired
  private Janus janusClient;

  @Override
  public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {

    final CoinUser coinUser = new CoinUser();

    coinUser.setDisplayName(credential.getAttributeAsString(DISPLAY_NAME_ATTTRIBUTE_NAME));
    coinUser.setEmail(credential.getAttributeAsString(EMAIL_ATTTRIBUTE_NAME));
    coinUser.setUid(credential.getAttributeAsString(UUID_ATTTRIBUTE_NAME));

    final String idpId = credential.getRemoteEntityID();
    coinUser.setInstitutionId(getInstitutionId(idpId));

    List<IdentityProvider> instituteIdPs = identityProviderService.getInstituteIdentityProviders(coinUser.getInstitutionId());
    if (!instituteIdPs.isEmpty()) {
      for (IdentityProvider idp : instituteIdPs) {
        coinUser.addInstitutionIdp(idp);
      }
    }
    // Add the one the user is currently identified by if it's not in the list
    // already.
    if (coinUser.getInstitutionIdps().isEmpty()) {
      IdentityProvider idp = getInstitutionIdP(idpId);
      coinUser.addInstitutionIdp(idp);
    }
    for (IdentityProvider idp : coinUser.getInstitutionIdps()) {
      if (idp.getId().equalsIgnoreCase(idpId)) {
        coinUser.setIdp(idp);
      }
    }

    Assert.notNull(coinUser, "No IdP ('" + idpId + "') could be identified from institution IdP's ('" + coinUser.getInstitutionIdps() + "')");


    LOG.debug("Done assembling user-details. Result: {}", coinUser);
    return coinUser;
  }

  private String getInstitutionId(final String idpId) {
    final IdentityProvider identityProvider = identityProviderService.getIdentityProvider(idpId);
    if (identityProvider != null) {
      final String institutionId = identityProvider.getInstitutionId();
      if (!StringUtils.isBlank(institutionId)) {
        return institutionId;
      }
    }
    throw new IllegalStateException("Unable to find an institutionId for IDP-id: " + idpId);
  }

  private IdentityProvider getInstitutionIdP(final String idpId) {
    IdentityProvider idp = identityProviderService.getIdentityProvider(idpId);
    if (idp == null) {
      final JanusEntity entity = janusClient.getEntity(idpId);
      if (entity == null) {
        idp = new IdentityProvider(idpId, null, idpId);
      } else {
        idp = new IdentityProvider(entity.getEntityId(), null, entity.getPrettyName());
      }
    }
    return idp;
  }
}
