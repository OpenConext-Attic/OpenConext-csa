package nl.surfnet.coin.csa.integration;

import nl.surfnet.coin.csa.CsaClient;
import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.oauth.ClientCredentialsClient;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * True integration test that runs against a remote environment.
 *
 * Name ending in 'Manual' to be ignored by the Maven Failsafe plugin.
 */
public class RemoteCsaTestIntegrationManual {//extends CsaClientTestIntegration {

  private static final String AUTHORIZATION_SERVER = "https://apis.showroom.surfconext.nl/";

  private static final String CSA_SERVER = "https://csa.showroom.surfconext.nl";
  public static final String CLIENT_KEY = "showroomsurfconextnl";
  public static final String CLIENT_SECRET = "??"; // Change accordingly

    protected static CsaClient csaClient;

    @BeforeClass
  public static void beforeClass() throws Exception {

    String csaOAuth2AuthorizationUrl = String.format("%s/oauth2/token", AUTHORIZATION_SERVER);
    csaClient = new CsaClient(CSA_SERVER);
    ClientCredentialsClient oauthClient = new ClientCredentialsClient();
    oauthClient.setClientKey(CLIENT_KEY);
    oauthClient.setClientSecret(CLIENT_SECRET);
    oauthClient.setOauthAuthorizationUrl(csaOAuth2AuthorizationUrl);
    csaClient.setOauthClient(oauthClient);
  }

    @Test
    public void testService() {
        Service service = csaClient.getServiceForIdp("https://surfguest.nl", 231);
        System.out.println(service);
    }
}
