/*
 * Copyright 2013 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.surfnet.coin.csa.api.control;

import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.domain.Provider.Language;
import nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor;
import nl.surfnet.coin.csa.service.IdentityProviderService;
import nl.surfnet.coin.csa.service.LmngService;
import nl.surfnet.coin.csa.service.impl.CompoundSPService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.sort;

@Controller
@RequestMapping
public class ServicesController extends BaseApiController{

  private
  @Value("${WEB_APPLICATION_CHANNEL}")
  String protocol;
  private
  @Value("${WEB_APPLICATION_HOST_AND_PORT}")
  String hostAndPort;
  private
  @Value("${WEB_APPLICATION_CONTEXT_PATH}")
  String contextPath;
  private
  @Value("${lmngDeepLinkBaseUrl}")
  String lmngDeepLinkBaseUrl;

  @Resource
  private CompoundSPService compoundSPService;

  @Resource
  private LmngService lmngService;

  @Resource
  private IdentityProviderService idpService;

  @Value("${public.api.lmng.guids}")
  private String[] guids;

  @RequestMapping(method = RequestMethod.GET, value = "/api/public/services.json")
  public
  @ResponseBody
  List<Service> getPublicServices(@RequestParam(value = "lang", defaultValue = "en") String language) {
    List<CompoundServiceProvider> csPs = compoundSPService.getAllPublicCSPs();
    List<Service> result = buildApiServices(csPs, language);

    // add public service from LMNG directly
    for (String guid : guids) {
      Article currentArticle = lmngService.getService(guid);
      //TODO: here we use id 0 as there is no CSP. How to handle this in the public API? We probably do not want to expose the ID at all there.
      Service currentPS = new Service(0L, currentArticle.getServiceDescriptionNl(), currentArticle.getDetailLogo(),
          null, true, lmngDeepLinkBaseUrl + guid);
      result.add(currentPS);
    }
    sort(result);
    return result;
  }

  private String getServiceLogo(CompoundServiceProvider csP) {
    String detailLogo = csP.getDetailLogo();
    if (detailLogo != null) {
      if (detailLogo.startsWith("/")) {
        detailLogo = protocol + "://" + hostAndPort + (StringUtils.hasText(contextPath) ? contextPath : "")
                + detailLogo;
      }
    }
    return detailLogo;
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/services.json")
  public
  @ResponseBody
  List<Service> getProtectedServices(@RequestParam(value = "lang", defaultValue = "en") String language,
                                     final HttpServletRequest request) {
    String ipdEntityId = getIdpEntityIdFromToken(request);
    return doGetServicesForIdP(language, ipdEntityId);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/idp/services.json")
  public
  @ResponseBody
  List<Service> getProtectedServicesByIdp(@RequestParam(value = "lang", defaultValue = "en") String language, @RequestParam(value = "idpEntityId") String idpEntityId,
                                          final HttpServletRequest request) {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_JIRA);
    return doGetServicesForIdP(language, idpEntityId);
  }

  private List<Service> doGetServicesForIdP(String language, String ipdEntityId) {
    IdentityProvider identityProvider = idpService.getIdentityProvider(ipdEntityId);
    List<CompoundServiceProvider> csPs = compoundSPService.getCSPsByIdp(identityProvider);
    List<CompoundServiceProvider> scopedSsPs = new ArrayList<CompoundServiceProvider>();
    /*
     * We only want the SP's that are currently linked to the IdP, not the also included SP's that are NOT IdP-only
     */
    for (CompoundServiceProvider csp : csPs) {
      if (csp.getServiceProvider().isLinked() && !csp.isHideInProtectedCsa()) {
        scopedSsPs.add(csp);
      }
    }
    List<Service> result = buildApiServices(scopedSsPs, language);

    sort(result);
    return result;
  }

  /**
   * Convert the list of found services to a list of services that can be
   * displayed in the API (either public or private)
   *
   * @param services list of services to convert (compound service providers)
   * @param language language to use in the result
   * @return a list of api services
   */
  private List<Service> buildApiServices(List<CompoundServiceProvider> services, String language) {
    List<Service> result = new ArrayList<Service>();
    boolean isEn = language.equalsIgnoreCase("en");
    for (CompoundServiceProvider csP : services) {
      String crmLink = csP.isArticleAvailable() ? (lmngDeepLinkBaseUrl + csP.getLmngId()) : null;
      result.add(new Service(csP.getId(), isEn ? csP.getSp().getName(Language.EN) : csP.getSp().getName(Language.NL),
          getServiceLogo(csP), csP.getServiceUrl(), csP.isArticleAvailable(), crmLink));
    }
    return result;
  }

}
