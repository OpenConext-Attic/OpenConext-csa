package nl.surfnet.coin.csa.model;

import java.util.List;

public class OfferedService {
  private final Service service;
  private final List<InstitutionIdentityProvider> identityProviders;

  public OfferedService(Service service, List<InstitutionIdentityProvider> identityProviders) {
    this.service = service;
    this.identityProviders = identityProviders;
  }

  public Service getService() {
    return service;
  }

  public List<InstitutionIdentityProvider> getIdentityProviders() {
    return identityProviders;
  }

  @Override
  public String toString() {
    return "OfferedService{" +
      "service=" + service +
      ", identityProviders=" + identityProviders +
      '}';
  }
}
