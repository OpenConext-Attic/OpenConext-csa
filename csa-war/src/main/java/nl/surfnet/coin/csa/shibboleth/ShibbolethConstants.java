package nl.surfnet.coin.csa.shibboleth;

/**
 * Lists the names under which Shibboleth makes SAML attributes available on the HttpServletRequest
 */
public final class ShibbolethConstants {

  public static final String DISPLAY_NAME = "Shib-displayName";
  public static final String EMAIL = "Shib-email";

  private ShibbolethConstants() {
  }
}
