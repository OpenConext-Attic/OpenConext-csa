package nl.surfnet.coin.csa.shibboleth;

/**
 * Represents the data about the user that is provided to us by Shibboleth
 */
public class ShibbolethPrincipal {

  private final String nameId;
  private final String displayName;
  private final String email;

  public ShibbolethPrincipal(String nameId, String displayName, String email) {
    this.nameId = nameId;
    this.displayName = displayName;
    this.email = email;
  }

  public String getNameId() {
    return nameId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmail() {
    return email;
  }

}
