package nl.surfnet.coin.csa.filter;

import com.google.common.base.Preconditions;
import nl.surfnet.coin.csa.domain.CheckTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AuthorizationServerFilter implements Filter {

  public static final String CHECK_TOKEN_RESPONSE = "CheckTokenResponse";

  private static final Logger LOG = LoggerFactory.getLogger(AuthorizationServerFilter.class);
  private static final String BEARER = "bearer";

  /*
     * Details needed so that we may check tokens presented to us by clients. This application uses them to authenticate via
     * Basic authentication with the oAuth server.
     */
  private String oauthCheckTokenEndpointUrl;
  private String oauthCheckTokenClientId;
  private String oauthCheckTokenSecret;

  private RestTemplate restTemplate = new RestTemplate();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    final String accessToken = getAccessToken(request);

    if (accessToken == null) {
      sendError(response, HttpServletResponse.SC_FORBIDDEN, "OAuth secured endpoint");
      return;
    }
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
    formData.add("token", accessToken);
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", getAuthorizationHeader(oauthCheckTokenClientId, oauthCheckTokenSecret));
    try {
      Map<String, Object> map = postForMap(oauthCheckTokenEndpointUrl, formData, headers);
      CheckTokenResponse tokenResponse = parseCheckTokenResponse(map);
      request.setAttribute(CHECK_TOKEN_RESPONSE, tokenResponse);
      chain.doFilter(request, response);
    } catch (HttpClientErrorException e) {
      sendError(response, HttpServletResponse.SC_FORBIDDEN, "invalid token");
    }
  }

  private CheckTokenResponse parseCheckTokenResponse(Map<String, Object> map) {
    List<String> scopes = (List<String>) map.get("scope");
    Preconditions.checkArgument(scopes != null, "Authorization server did not return an 'scope' value");

    /*
     * authenticatingAuthority is optional for client-credential clients
     */
    String authenticatingAuthority = (String) map.get("authenticatingAuthority");
    return new CheckTokenResponse(authenticatingAuthority, scopes);
  }

  @Override
  public void destroy() {
  }

  private String getAuthorizationHeader(String clientId, String clientSecret) {
    String creds = String.format("%s:%s", clientId, clientSecret);
    try {
      return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Could not convert String");
    }
  }

  private Map<String, Object> postForMap(String path, MultiValueMap<String, String> formData, org.springframework.http.HttpHeaders headers) {
    if (headers.getContentType() == null) {
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    }
    return restTemplate.exchange(path, HttpMethod.POST,
      new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class).getBody();
  }

  private String getAccessToken(HttpServletRequest request) {
    String accessToken = null;
    String header = request.getHeader("Authorization");
    if (header != null) {
      int space = header.indexOf(' ');
      if (space > 0) {
        String method = header.substring(0, space);
        if (BEARER.equalsIgnoreCase(method)) {
          accessToken = header.substring(space + 1);
        }
      }
    }
    return accessToken;
  }

  private void sendError(HttpServletResponse response, int statusCode, String reason) {
    LOG.warn("No valid access-token on request. Will respond with error response: {} {}", statusCode, reason);
    try {
      response.sendError(statusCode, reason);
      response.flushBuffer();
    } catch (IOException e) {
      throw new RuntimeException(reason, e);
    }
  }

  public void setOauthCheckTokenEndpointUrl(String oauthCheckTokenEndpointUrl) {
    this.oauthCheckTokenEndpointUrl = oauthCheckTokenEndpointUrl;
  }

  public void setOauthCheckTokenClientId(String oauthCheckTokenClientId) {
    this.oauthCheckTokenClientId = oauthCheckTokenClientId;
  }

  public void setOauthCheckTokenSecret(String oauthCheckTokenSecret) {
    this.oauthCheckTokenSecret = oauthCheckTokenSecret;
  }

}
