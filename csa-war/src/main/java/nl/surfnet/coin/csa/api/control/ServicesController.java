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

import nl.surfnet.coin.csa.dao.FacetDao;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.domain.Provider.Language;
import nl.surfnet.coin.csa.domain.Screenshot;
import nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor;
import nl.surfnet.coin.csa.model.*;
import nl.surfnet.coin.csa.service.CrmService;
import nl.surfnet.coin.csa.service.IdentityProviderService;
import nl.surfnet.coin.csa.service.impl.CompoundSPService;
import org.apache.commons.collections.CollectionUtils;
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
public class ServicesController extends BaseApiController {

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
  private CrmService lmngService;

  @Resource
  private FacetDao facetDao;

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
              null, true, lmngDeepLinkBaseUrl + guid, null);
      result.add(currentPS);
    }
    sort(result);
    return result;
  }

  /**
   * Returns an absolute URL for the given url
   */
  private String absoluteUrl(final String relativeUrl) {
    String result = relativeUrl;
    if (result != null) {
      if (result.startsWith("/")) {
        result = protocol + "://" + hostAndPort + (StringUtils.hasText(contextPath) ? contextPath : "") + result;
      }
    }
    return result;
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
  List<Service> getProtectedServicesByIdp(
          @RequestParam(value = "lang", defaultValue = "en") String language,
          @RequestParam(value = "idpEntityId") String idpEntityId,
          final HttpServletRequest request) {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES);
    return doGetServicesForIdP(language, idpEntityId);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/services/{serviceId}.json")
  public
  @ResponseBody
  Service getServiceForIdp(
          @PathVariable("serviceId") long serviceId,
          @RequestParam(value = "lang", defaultValue = "en") String language,
          @RequestParam(value = "idpEntityId") String idpEntityId,
          final HttpServletRequest request) {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES);
    IdentityProvider identityProvider = idpService.getIdentityProvider(idpEntityId);
    CompoundServiceProvider csp = compoundSPService.getCSPById(identityProvider, serviceId, false);
    return buildApiService(csp, language);
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
    for (CompoundServiceProvider csp : services) {
      result.add(buildApiService(csp, language));
    }
    return result;
  }

  /**
   * Build a Service object based on the given CSP
   */
  private Service buildApiService(CompoundServiceProvider csp, String language) {
    boolean isEn = language.equalsIgnoreCase("en");

    Service service = new Service();

    // Plain properties
    service.setSpEntityId(csp.getSp().getId());
    service.setAppUrl(csp.getAppUrl());
    service.setServiceUrl(csp.getAppUrl()); // TODO: duplicates AppUrl?
    service.setId(csp.getId());
    service.setEulaUrl(csp.getEulaUrl());
    service.setCrmUrl(csp.isArticleAvailable() ? (lmngDeepLinkBaseUrl + csp.getLmngId()) : null);
    service.setDetailLogoUrl(absoluteUrl(csp.getDetailLogo()));
    service.setLogoUrl(absoluteUrl(csp.getAppStoreLogo()));
    service.setSupportMail(csp.getSupportMail());
    service.setWebsiteUrl(csp.getSp().getHomeUrl()); // TODO: verify whether this is the correct property in the SP
    service.setConnected(csp.getSp().isLinked());
    service.setArp(csp.getSp().getArp());

    // Screenshots
    if (CollectionUtils.isNotEmpty(csp.getScreenShotsImages())) {
      List<String> screenshots = new ArrayList<String>();
      for (Screenshot screenshot : csp.getScreenShotsImages()) {
        screenshots.add(absoluteUrl(screenshot.getFileUrl()));
      }
      service.setScreenshotUrls(screenshots);
    }

    // Language-specific properties
    if (isEn) {
      service.setDescription(csp.getServiceDescriptionEn());
      service.setEnduserDescription(csp.getEnduserDescriptionEn());
      service.setName(csp.getSp().getName(Language.EN));
      service.setSupportUrl(csp.getSupportUrlEn());
      service.setInstitutionDescription(csp.getInstitutionDescriptionEn());
    } else {
      service.setEnduserDescription(csp.getEnduserDescriptionNl());
      service.setName(csp.getSp().getName(Language.NL));
      service.setSupportUrl(csp.getSupportUrlNl());
      service.setInstitutionDescription(csp.getInstitutionDescriptionNl());
    }


    // CRM-related properties
    if (csp.isArticleAvailable()) {
      CrmArticle crmArticle = new CrmArticle();
      crmArticle.setGuid(csp.getArticle().getLmngIdentifier());
      if (csp.getArticle().getAndroidPlayStoreMedium() != null) {
        crmArticle.setAndroidPlayStoreUrl(csp.getArticle().getAndroidPlayStoreMedium().getUrl());
      }
      if (csp.getArticle().getAppleAppStoreMedium() != null) {
        crmArticle.setAppleAppStoreUrl(csp.getArticle().getAppleAppStoreMedium().getUrl());
      }
      service.setHasCrmLink(true);
      service.setCrmArticle(crmArticle);
    }

    // License-related
    if (csp.isLicenseAvailable()) {
      License l = new License();
      l.setEndDate(csp.getLicense().getEndDate());
      l.setStartDate(csp.getLicense().getStartDate());
      l.setGroupLicense(csp.getLicense().isGroupLicense());
      l.setLicenseNumber(csp.getLicense().getLicenseNumber());
      l.setInstitutionName(csp.getLicense().getInstitutionName());
      service.setLicense(l);
    }

    // WIP: categories
    List<Category> categories = new ArrayList<Category>();
    for (FacetValue facetValue : csp.getFacetValues()) {
      Facet facet = facetValue.getFacet();
      Category category = findCategory(categories, facet);
      if (category == null) {
        category = new Category(facet.getName());
        categories.add(category);
      }
      category.addCategoryValue(new CategoryValue(facetValue.getValue()));
    }
    service.setCategories(categories);
    return service;
  }

  private Category findCategory(List<Category> categories, Facet facet) {
    for (Category category : categories) {
      if (category.getName().equalsIgnoreCase(facet.getName())) {
        return category;
      }
    }
    return null;
  }

}
