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


import nl.surfnet.coin.csa.api.cache.CrmCache;
import nl.surfnet.coin.csa.api.cache.ProviderCache;
import nl.surfnet.coin.csa.api.cache.ServicesCache;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.Screenshot;
import nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor;
import nl.surfnet.coin.csa.model.*;
import nl.surfnet.coin.csa.service.CrmService;
import nl.surfnet.coin.csa.service.impl.CompoundSPService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping
public class ServicesController extends BaseApiController implements ServicesService {

  private static final Logger LOG = LoggerFactory.getLogger(ServicesController.class);

  @Resource
  private ServicesCache servicesCache;

  @Resource
  private ProviderCache providerCache;

  @Resource
  private CrmCache crmCache;

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


  @Value("${public.api.lmng.guids}")
  private String[] guids;


  @Override
  public Map<String, List<Service>> findAll() {
    List<CompoundServiceProvider> allCSPs = compoundSPService.getAllCSPs();
    List<Service> servicesEn = buildApiServices(allCSPs, "en");
    List<Service> servicesNl = buildApiServices(allCSPs, "nl");
    List<Service> crmOnlyServices = getCrmOnlyServices();
    servicesEn.addAll(crmOnlyServices);
    servicesNl.addAll(crmOnlyServices);
    Map<String, List<Service>> result = new HashMap<String, List<Service>>();
    result.put("en", servicesEn);
    result.put("nl", servicesNl);
    return result;
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/public/services.json")
  public
  @ResponseBody
  List<Service> getPublicServices(@RequestParam(value = "lang", defaultValue = "en") String language) {
    List<Service> allServices = servicesCache.getAllServices(language);
    List<Service> publicServices = new ArrayList<Service>();
    for (Service service : allServices) {
      if (service.isAvailableForEndUser() && !service.isIdpVisibleOnly()) {
        publicServices.add(service);
      }
    }
    return publicServices;
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/services.json")
  public
  @ResponseBody
  List<Service> getProtectedServices(@RequestParam(value = "lang", defaultValue = "en") String language,
                                     final HttpServletRequest request) {
    String ipdEntityId = getIdpEntityIdFromToken(request);
    /*
     * Non-client-credential client where we only return linked services
     */
    return doGetServicesForIdP(language, ipdEntityId, false);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/service.json")
  public
  @ResponseBody
  Service getServiceForSpEntityId(@RequestParam(value = "lang", defaultValue = "en") String language,
      @RequestParam(value="idpEntityId") String idpEntityId,
      @RequestParam(value="spEntityId") String spEntityId,
                                     final HttpServletRequest request) {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES);
    CompoundServiceProvider csp = compoundSPService.getCSPByServiceProviderEntityId(spEntityId);
    return buildApiService(csp, language);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/idp/services.json")
  public
  @ResponseBody
  List<Service> getProtectedServicesByIdp(
          @RequestParam(value = "lang", defaultValue = "en") String language,
          @RequestParam(value = "idpEntityId") String idpEntityId,
          final HttpServletRequest request) {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES);
    /*
     * Client-credential client where we also return non-linked services (e.g. dashboard functionality)
     */
    return doGetServicesForIdP(language, idpEntityId, true);
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
    List<Service> allServices = servicesCache.getAllServices(language);
    for (Service service : allServices) {
      if (service.getId() == serviceId) {
        return service;
      }
    }
    throw new RuntimeException("Non-existent service ID('" + serviceId + "')");
  }

  private List<Service> doGetServicesForIdP(String language, String idpEntityId, boolean includeNotLinkedSPs) {
    List<String> serviceProviderIdentifiers = providerCache.getServiceProviderIdentifiers(idpEntityId);
    List<Service> allServices = servicesCache.getAllServices(language);
    List<Service> result = new ArrayList<Service>();
    for (Service service : allServices) {
      boolean isConnected = serviceProviderIdentifiers.contains(service.getSpEntityId());
      if ((service.isAvailableForEndUser() && isConnected) || includeNotLinkedSPs) {

        // Weave with 'is connected' from sp/idp matrix cache
        service.setConnected(isConnected);

        // Weave with article and license cache
        service.setLicense(crmCache.getLicense(service, idpEntityId));
        addArticle(crmCache.getArticle(service), service);

        result.add(service);
      }
    }
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
  public Service buildApiService(CompoundServiceProvider csp, String language) {
    boolean isEn = language.equalsIgnoreCase("en");

    Service service = new Service();
    plainProperties(csp, service);
    screenshots(csp, service);
    languageSpecificProperties(csp, isEn, service);
    addArticle(csp.getArticle(), service);
    service.setLicense(csp.getLicense());
    categories(csp, service);
    return service;
  }

  private void addArticle(Article article, Service service) {
    // CRM-related properties
    if (article != null && !article.equals(Article.NONE)) {
      CrmArticle crmArticle = new CrmArticle();
      crmArticle.setGuid(article.getLmngIdentifier());
      if (article.getAndroidPlayStoreMedium() != null) {
        crmArticle.setAndroidPlayStoreUrl(article.getAndroidPlayStoreMedium().getUrl());
      }
      if (article.getAppleAppStoreMedium() != null) {
        crmArticle.setAppleAppStoreUrl(article.getAppleAppStoreMedium().getUrl());
      }
      service.setHasCrmLink(true);
      service.setCrmArticle(crmArticle);
    }

  }

  private void categories(CompoundServiceProvider csp, Service service) {
    // Categories
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
  }

  private void languageSpecificProperties(CompoundServiceProvider csp, boolean en, Service service) {
    // Language-specific properties
    if (en) {
      service.setDescription(csp.getServiceDescriptionEn());
      service.setEnduserDescription(csp.getEnduserDescriptionEn());
      service.setName(csp.getTitleEn());
      service.setSupportUrl(csp.getSupportUrlEn());
      service.setInstitutionDescription(csp.getInstitutionDescriptionEn());
      service.setServiceUrl(csp.getSupportUrlEn());
    } else {
      service.setDescription(csp.getServiceDescriptionNl());
      service.setEnduserDescription(csp.getEnduserDescriptionNl());
      service.setName(csp.getTitleNl());
      service.setSupportUrl(csp.getSupportUrlNl());
      service.setInstitutionDescription(csp.getInstitutionDescriptionNl());
      service.setServiceUrl(csp.getSupportUrlNl());
    }
  }

  private void screenshots(CompoundServiceProvider csp, Service service) {
    // Screenshots
    if (CollectionUtils.isNotEmpty(csp.getScreenShotsImages())) {
      List<String> screenshots = new ArrayList<String>();
      for (Screenshot screenshot : csp.getScreenShotsImages()) {
        screenshots.add(absoluteUrl(screenshot.getFileUrl()));
      }
      service.setScreenshotUrls(screenshots);
    }
  }

  private void plainProperties(CompoundServiceProvider csp, Service service) {
    // Plain properties
    service.setSpEntityId(csp.getSp().getId());
    service.setAppUrl(csp.getAppUrl());
    service.setId(csp.getId());
    service.setEulaUrl(csp.getEulaUrl());
    service.setCrmUrl(csp.isArticleAvailable() ? (lmngDeepLinkBaseUrl + csp.getLmngId()) : null);
    service.setDetailLogoUrl(absoluteUrl(csp.getDetailLogo()));
    service.setLogoUrl(absoluteUrl(csp.getAppStoreLogo()));
    service.setSupportMail(csp.getSupportMail());
    service.setWebsiteUrl(csp.getServiceUrl());
    service.setArp(csp.getSp().getArp());
    service.setAvailableForEndUser(csp.isAvailableForEndUser());
    service.setIdpVisibleOnly(csp.getSp().isIdpVisibleOnly());
  }

  private Category findCategory(List<Category> categories, Facet facet) {
    for (Category category : categories) {
      if (category.getName().equalsIgnoreCase(facet.getName())) {
        return category;
      }
    }
    return null;
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

  private List<Service> getCrmOnlyServices() {
    List<Service> result = new ArrayList<Service>();
    for (String guid : guids) {
      Article currentArticle = lmngService.getService(guid);
      if (currentArticle == null) {
        LOG.info("A GUID has been configured that cannot be found in CRM: {}", guid);
      } else {
        Service currentPS = new Service(0L, currentArticle.getServiceDescriptionNl(), currentArticle.getDetailLogo(),
                null, true, lmngDeepLinkBaseUrl + guid, null);
        addArticle(currentArticle, currentPS);
        result.add(currentPS);
      }
    }
    return result;
  }

}
