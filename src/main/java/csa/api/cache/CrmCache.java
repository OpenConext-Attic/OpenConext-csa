package csa.api.cache;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import csa.domain.MappingEntry;
import csa.dao.LmngIdentifierDao;
import csa.domain.Article;
import csa.domain.IdentityProvider;
import csa.model.License;
import csa.model.Service;
import csa.service.CrmService;

@Component
public class CrmCache extends AbstractCache {

  private static final Logger LOG = LoggerFactory.getLogger(CrmCache.class);

  private final CrmService crmService;
  private final LmngIdentifierDao lmngIdentifierDao;

  @Autowired
  public CrmCache(CrmService crmService, LmngIdentifierDao lmngIdentifierDao,
                  @Value("${cacheMillisecondsStartupDelayTime}") long delay,
                  @Value("${cacheMillisecondsServices}") long duration) {
    super(delay, duration);
    this.crmService = crmService;
    this.lmngIdentifierDao = lmngIdentifierDao;
  }

  /**
   * Cache of Licenses, keyed by Entry of Idp institutionId and spEntityId
   */
  private ConcurrentMap<MappingEntry, License> licenseCache = new ConcurrentHashMap<>();

  /**
   * Cache of Articles, keyed by spEntityId
   */
  private ConcurrentMap<String, Article> articleCache = new ConcurrentHashMap<>();

  private List<MappingEntry> idpToLmngId;
  private List<MappingEntry> spToLmngId;

  private final Object lock = new Object();


  @Override
  protected void doPopulateCache() {
    synchronized (lock) {
      populateMappings();
      licenseCache = createNewLicensesCache();
      articleCache = createNewArticleCache();
    }
  }

  private void populateMappings() {
    idpToLmngId = lmngIdentifierDao.findAllIdentityProviders();
    spToLmngId = lmngIdentifierDao.findAllServiceProviders();

  }

  private ConcurrentMap<String, Article> createNewArticleCache() {
    ConcurrentMap<String, Article> newCache = new ConcurrentHashMap<>();

    // Here we only have to query the articles that have been mapped to an SP, luckily not the whole CRM database.
    for (MappingEntry spAndLmngId : spToLmngId) {
      String spEntityId = spAndLmngId.getKey();
      String lmngId = spAndLmngId.getValue();
      List<Article> articlesForServiceProviders = crmService.getArticlesForServiceProviders(Collections.singletonList(spEntityId));

      if (articlesForServiceProviders.size() > 1) {
        LOG.info("Unexpected: list of articles for SP ({}) is larger than 1: {}", spEntityId, articlesForServiceProviders);
        for (Article a : articlesForServiceProviders) {
          LOG.info("Article found: {}", a);
        }
      }

      if (articlesForServiceProviders.size() >= 1) {
        newCache.put(spEntityId, articlesForServiceProviders.get(0));
      } else {
        LOG.info("No article found for SP {}, with lmng id: {}", spEntityId, lmngId);
      }
    }
    return newCache;
  }

  private ConcurrentMap<MappingEntry, License> createNewLicensesCache() {
    ConcurrentMap<MappingEntry, License> newLicenseCache = new ConcurrentHashMap<>();
    // Nested loop to query the cartesian product of all SPs and all IdPs
    for (MappingEntry idpLmngEntry : idpToLmngId) {
      String idpInstitutionId = idpLmngEntry.getKey();
      for (MappingEntry spLmngEntry : spToLmngId) {
        String spEntityId = spLmngEntry.getKey();
        String spLmngId = spLmngEntry.getValue();

        IdentityProvider idp = new IdentityProvider(idpInstitutionId, idpInstitutionId, "dummy");

        List<License> licensesForIdpAndSp = crmService.getLicensesForIdpAndSp(idp, spLmngId);
        if (licensesForIdpAndSp.size() > 0) {
          if (licensesForIdpAndSp.size() > 1) {
            LOG.warn("Unexpected: list of licenses by IdP and SP ({} and {}) is larger than 1: {}", idpInstitutionId, spEntityId, licensesForIdpAndSp.size());
          }
          License license = licensesForIdpAndSp.get(0);
          if (LOG.isTraceEnabled()) {
            LOG.trace("License found by IdP and SP ({} and {}): {}", idpInstitutionId, spEntityId, license);
          }
          newLicenseCache.put(new MappingEntry(idpInstitutionId, spEntityId), license);
        } else {
          if (LOG.isTraceEnabled()) {
            LOG.trace("No result found for licenses by IdP and SP ({} and {})", idpInstitutionId, spEntityId);
          }
        }
      }
    }
    return newLicenseCache;
  }

  public License getLicense(Service service, String idpInstitutionId) {
    if (service.getSpEntityId() == null || idpInstitutionId == null) {
      /*
       * First check:
       *
       * If this is the case then the Service is based upon a CRM guid defined in the csa.properties (key=public.api.lmng.guids). It must be displayed in the
       * services API , however it can't have a license as this is per-sp basis.
       *
       * Second check:
       *
       * It is possible that in SR there is no registered institutionID for an IdP. This is a misconfiguration, but not something we want to bring to the attention here.
       */
      return null;
    }
    MappingEntry entry = new MappingEntry(idpInstitutionId, service.getSpEntityId());
    License license = licenseCache.get(entry);
    LOG.debug("Looked for license for service {} and idpInstitutionId {}, and found: {}", service.getSpEntityId(), idpInstitutionId, license);
    return license;
  }

  public Article getArticle(Service service) {
    if (service.getSpEntityId() == null) {
      // This happens for 'crm only' services, with no reference to Service Registry's services.
      return null;
    }
    return SerializationUtils.clone(articleCache.get(service.getSpEntityId()));
  }

  @Override
  protected String getCacheName() {
    return "CRM Cache";
  }

}
