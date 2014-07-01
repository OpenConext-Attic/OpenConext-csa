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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import nl.surfnet.coin.csa.api.cache.CrmCache;
import nl.surfnet.coin.csa.api.cache.ProviderCache;
import nl.surfnet.coin.csa.api.cache.ServicesCache;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.domain.Screenshot;
import nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor;
import nl.surfnet.coin.csa.model.Category;
import nl.surfnet.coin.csa.model.CategoryValue;
import nl.surfnet.coin.csa.model.CrmArticle;
import nl.surfnet.coin.csa.model.Facet;
import nl.surfnet.coin.csa.model.FacetValue;
import nl.surfnet.coin.csa.model.InstitutionIdentityProvider;
import nl.surfnet.coin.csa.model.OfferedService;
import nl.surfnet.coin.csa.model.Service;
import nl.surfnet.coin.csa.service.CrmService;
import nl.surfnet.coin.csa.service.IdentityProviderService;
import nl.surfnet.coin.csa.service.impl.CompoundSPService;


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

  @SuppressWarnings("MismatchedReadAndWriteOfArray")
  @Value("${public.api.lmng.guids}")
  private String[] guids;

  @Resource
  private IdentityProviderService identityProviderService;

  @Override
  public Map<String, List<Service>> findAll(long callDelay) {
    List<CompoundServiceProvider> allCSPs = compoundSPService.getAllCSPs(callDelay);
    List<Service> servicesEn = buildApiServices(allCSPs, "en");
    List<Service> servicesNl = buildApiServices(allCSPs, "nl");
    List<Service> crmOnlyServices = getCrmOnlyServices();
    servicesEn.addAll(crmOnlyServices);
    servicesNl.addAll(crmOnlyServices);
    Map<String, List<Service>> result = new HashMap<>();
    result.put("en", servicesEn);
    result.put("nl", servicesNl);
    return result;
  }

  @Override
  public Map<String, List<Service>> findAll() {
    return findAll(0L);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/public/services.json")
  public
  @ResponseBody
  List<Service> getPublicServices(@RequestParam(value = "lang", defaultValue = "en") String language) {
    List<Service> allServices = servicesCache.getAllServices(language);
    List<Service> publicServices = new ArrayList<>();
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
                                  @RequestParam(value = "idpEntityId") String idpEntityId,
                                  @RequestParam(value = "spEntityId") String spEntityId,
                                  final HttpServletRequest request) {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES);
    List<Service> allServices = doGetServicesForIdP(language, idpEntityId, true);
    for (Service service : allServices) {
      if (service.getSpEntityId().equals(spEntityId)) {
        return service;
      }
    }
    throw new RuntimeException("Non-existent service by sp entity id '" + spEntityId + "'");
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

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/idp/offered-services.json")
  public @ResponseBody List<OfferedService> getOfferedServicesByIdp(
    @RequestParam(value = "idpEntityId") final String idpEntityId,
    @RequestParam(value = "lang", defaultValue = "en") String language,
    final HttpServletRequest request) {

    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES);
    final IdentityProvider identityProvider = providerCache.getIdentityProvider(idpEntityId);
    if (identityProvider == null) {
      throw new IllegalArgumentException("No IdentityProvider known in SR with name:'" + idpEntityId + "'");
    }
    final List<Service> allServices = servicesCache.getAllServices(language);
    LOG.debug("Total of {} services known", allServices.size());
    LOG.debug("identity whose services we look up: {}", identityProvider);
    final Collection<Service> myServices = Collections2.filter(allServices, new Predicate<Service>() {
      @Override
      public boolean apply(final Service input) {
        LOG.debug("Candidate service id: {}, Service-institutionId: {}, ", input.getSpEntityId(), input.getInstitutionId());
        return identityProvider.getInstitutionId() != null && identityProvider.getInstitutionId().equals(input.getInstitutionId());
      }
    });
    LOG.debug("Idp with id {} offers {} services", idpEntityId, myServices.size());

    List<OfferedService> result = new ArrayList<>();
    final List<IdentityProvider> allIdentityProviders = identityProviderService.getAllIdentityProviders();

    for (final Service myOfferedService: myServices) {
      List<InstitutionIdentityProvider> usingInstitutions = new ArrayList<>();
      for (IdentityProvider idp: allIdentityProviders) {
        final List<String> linkedServiceProviderIDs = servicesCache.findUsedServiceProvidersIds(identityProvider);
        // look up optional users of our services
        if (linkedServiceProviderIDs.contains(myOfferedService.getSpEntityId())) {
          usingInstitutions.add(new InstitutionIdentityProvider(idp.getId(), idp.getName(), idp.getInstitutionId()));
        }
      }
      final OfferedService offeredService = new OfferedService(myOfferedService, usingInstitutions);
      LOG.debug("Found offered service {}", offeredService);
      result.add(offeredService);
    }

    LOG.debug("Number of offered services found: {}", result.size());
    return result;

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
    List<Service> allServices = doGetServicesForIdP(language, idpEntityId, true);
    for (Service service : allServices) {
      if (service.getId() == serviceId) {
        return service;
      }
    }
    throw new RuntimeException("Non-existent service ID('" + serviceId + "')");
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/cache/clear.json")
  public
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  void clearCache(final HttpServletRequest request) {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_CROSS_IDP_SERVICES);
    this.providerCache.evictSynchronously();
    this.servicesCache.evictSynchronously();
  }

  private List<Service> doGetServicesForIdP(String language, String idpEntityId, boolean includeNotLinkedSPs) {
    IdentityProvider identityProvider = providerCache.getIdentityProvider(idpEntityId);
    if (identityProvider == null) {
      throw new IllegalArgumentException("No IdentityProvider known in SR with name:'" + idpEntityId + "'");
    }
    List<String> serviceProviderIdentifiers = providerCache.getServiceProviderIdentifiers(idpEntityId);

    List<Service> allServices = servicesCache.getAllServices(language);
    List<Service> result = new ArrayList<>();
    for (Service service : allServices) {
      boolean isConnected = serviceProviderIdentifiers.contains(service.getSpEntityId());
      if ((service.isAvailableForEndUser() && isConnected) || (includeNotLinkedSPs && !service.isIdpVisibleOnly())) {

        // Weave with 'is connected' from sp/idp matrix cache
        service.setConnected(isConnected);

        // Weave with article and license from caches
        String institutionId = identityProvider.getInstitutionId();
        service.setLicense(crmCache.getLicense(service, institutionId));
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
    List<Service> result = new ArrayList<>();
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
    categories(csp, service, language);
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

  private void categories(CompoundServiceProvider csp, Service service, String locale) {
    // Categories - the category values need to be either in nl or en (as the facet and facet_values are based on the language setting)
    List<Category> categories = new ArrayList<>();
    for (FacetValue facetValue : csp.getFacetValues()) {
      Facet facet = facetValue.getFacet();
      Category category = findCategory(categories, facet);
      if (category == null) {
        category = new Category(facet.getLocaleName(locale));
        categories.add(category);
      }
      category.addCategoryValue(new CategoryValue(facetValue.getLocaleValue(locale)));
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
      List<String> screenshots = new ArrayList<>();
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
    service.setInstitutionId(csp.getSp().getInstitutionId());
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
    List<Service> result = new ArrayList<>();
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
