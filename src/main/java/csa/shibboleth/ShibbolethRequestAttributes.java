package csa.shibboleth;

/**
 * Lists the names under which Shibboleth makes SAML attributes available on the HttpServletRequest
 */
public enum ShibbolethRequestAttributes {

  UID("Shib-user"), DISPLAY_NAME("Shib-displayName"), EMAIL("Shib-email"), IDP_ID("Shib-Identity-Provider");

  private final String attributeName;

  public String getAttributeName() {
    return attributeName;
  }

  ShibbolethRequestAttributes(String attributeName) {
    this.attributeName = attributeName;
  }
}
