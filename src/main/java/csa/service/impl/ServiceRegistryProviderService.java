/*
 * Copyright 2012 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csa.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;

import csa.domain.ContactPerson;
import csa.domain.ServiceProvider;
import csa.janus.Janus;
import csa.janus.domain.Contact;
import csa.janus.domain.EntityMetadata;
import csa.janus.domain.JanusEntity;
import csa.domain.ContactPersonType;
import csa.domain.IdentityProvider;
import csa.janus.domain.ARP;
import csa.service.IdentityProviderService;
import csa.service.ServiceProviderService;

@Service
public class ServiceRegistryProviderService implements ServiceProviderService, IdentityProviderService {

  private static final Logger log = LoggerFactory.getLogger(ServiceRegistryProviderService.class);
  private static final String IN_PRODUCTION = "prodaccepted";

  public void setJanusClient(Janus janusClient) {
    this.janusClient = janusClient;
  }

  @Autowired
  private Janus janusClient;

  @Override
  public List<ServiceProvider> getAllServiceProviders(String idpId) {
    List<ServiceProvider> allSPs = getAllServiceProvidersUnfiltered(false, 0);

    List<String> myLinkedSPs = getLinkedServiceProviderIDs(idpId);

    List<ServiceProvider> filteredList = new ArrayList<>();
    for (ServiceProvider sp : allSPs) {
      if (myLinkedSPs.contains(sp.getId())) {
        // an already linked SP is visible
        sp.setLinked(true);
        filteredList.add(sp);
      } else if (!sp.isIdpVisibleOnly()) {
        // Not-linked sps are only visible if 'idp visible only' is not true.
        filteredList.add(sp);
      }
    }
    return filteredList;
  }

  @Override
  public List<ServiceProvider> getAllServiceProviders(boolean includeArps) {
    return getAllServiceProvidersUnfiltered(includeArps, 0);
  }

  @Override
  public List<ServiceProvider> getAllServiceProvidersRateLimited(long rateDelay) {
    return getAllServiceProvidersUnfiltered(true, rateDelay);
  }

  @Override
  public List<String> getLinkedServiceProviderIDs(String idpId) {
    List<String> spList = new ArrayList<>();
    try {
      spList = janusClient.getAllowedSps(idpId);
    } catch (RestClientException e) {
      log.error("Could not retrieve allowed SPs from Janus client", e);
    }
    return spList;
  }

  private List<ServiceProvider> getAllServiceProvidersUnfiltered(boolean includeArps, long callDelay) {
    List<ServiceProvider> spList = new ArrayList<>();
    try {
      final List<EntityMetadata> sps = janusClient.getSpList();
      for (EntityMetadata metadata : sps) {
        if (StringUtils.equals(metadata.getWorkflowState(), IN_PRODUCTION)) {
          spList.add(buildServiceProviderByMetadata(metadata, includeArps));
          if (callDelay > 0) {
            try {
              Thread.sleep(callDelay);
            } catch (InterruptedException e) {
              break;
            }
          }
        }
      }
    } catch (RestClientException e) {
      log.error("Could not retrieve 'all SPs' from Janus client", e);
    }
    return spList;
  }

  @Override
  public ServiceProvider getServiceProvider(String spEntityId, String idpEntityId) {
    try {
      // first get JanusEntity. This holds the information about the workflow
      // only allow production status
      final JanusEntity entity = janusClient.getEntity(spEntityId);
      if (entity == null || !(IN_PRODUCTION.equals(entity.getWorkflowStatus()))) {
        return null;
      }
      // Get the metadata and build a ServiceProvider with this metadata
      EntityMetadata metadata = janusClient.getMetadataByEntityId(spEntityId);
      final ServiceProvider serviceProvider = buildServiceProviderByMetadata(metadata, true);

      // Check if the IdP can connect to this service
      if (idpEntityId != null) {
        final boolean linked = janusClient.isConnectionAllowed(spEntityId, idpEntityId);
        serviceProvider.setLinked(linked);
      }

      return serviceProvider;
    } catch (RestClientException e) {
      log.error("Could not retrieve metadata from Janus client", e);
    }
    return null;
  }

  @Override
  public ServiceProvider getServiceProvider(String spEntityId) {
    return getServiceProvider(spEntityId, null);
  }

  /**
   * Create a ServiceProvider and inflate it with the given metadata attributes.
   *
   * @param metadata    Janus metadata
   * @return {@link ServiceProvider}
   */
  public ServiceProvider buildServiceProviderByMetadata(EntityMetadata metadata, boolean includeArps) {
    Assert.notNull(metadata, "metadata cannot be null");
    final String appEntityId = metadata.getAppEntityId();
    String name = metadata.getNames().get("en");
    if (StringUtils.isBlank(name)) {
      name = appEntityId;
    }
    ServiceProvider sp = new ServiceProvider(appEntityId);
    // this is needed for sorting
    sp.setName(name);
    sp.setNames(metadata.getNames());
    sp.setLogoUrl(metadata.getAppLogoUrl());
    sp.setHomeUrls(metadata.getAppHomeUrls());
    sp.setDescriptions(metadata.getDescriptions());
    sp.setIdpVisibleOnly(metadata.isIdpVisibleOnly());
    sp.setEulaURL(metadata.getEula());
    sp.setUrls(metadata.getUrls());
    sp.setApplicationUrl(metadata.getApplicationUrl());
    sp.setGadgetBaseUrl(metadata.getOauthConsumerKey());
    sp.setInstitutionId(metadata.getInstutionId());
    sp.setPublishedInEdugain(metadata.isPublishedInEduGain());
    for (Contact c : metadata.getContacts()) {
      ContactPerson p = new ContactPerson(StringUtils.join(new Object[]{c.getGivenName(), c.getSurName()}, " "), c.getEmailAddress());
      p.setContactPersonType(contactPersonTypeByJanusContactType(c.getType()));
      p.setTelephoneNumber(c.getTelephoneNumber());
      sp.addContactPerson(p);
    }
    // Get the ARP (if there is any)
    if (includeArps) {
      final ARP arp = janusClient.getArp(appEntityId);
      sp.setArp(arp);
    }
    return sp;
  }

  /**
   * Create a IdentityProvider and inflate it with the given metadata
   * attributes.
   *
   * @param metadata Janus metadata
   * @return {@link IdentityProvider}
   */
  public static IdentityProvider buildIdentityProviderByMetadata(EntityMetadata metadata) {
    Assert.notNull(metadata, "metadata cannot be null");
    final String appEntityId = metadata.getAppEntityId();
    String name = metadata.getNames().get("en");
    if (StringUtils.isBlank(name)) {
      name = appEntityId;
    }
    IdentityProvider idp = new IdentityProvider(appEntityId, metadata.getInstutionId(), name);
    // this is needed for sorting
    idp.setName(name);
    idp.setNames(metadata.getNames());
    idp.setLogoUrl(metadata.getAppLogoUrl());
    idp.setHomeUrls(metadata.getAppHomeUrls());
    idp.setDescriptions(metadata.getDescriptions());
    for (Contact c : metadata.getContacts()) {
      ContactPerson p = new ContactPerson(StringUtils.join(new Object[]{c.getGivenName(), c.getSurName()}, " "), c.getEmailAddress());
      p.setContactPersonType(contactPersonTypeByJanusContactType(c.getType()));
      p.setTelephoneNumber(c.getTelephoneNumber());
      idp.addContactPerson(p);
    }
    return idp;
  }

  /**
   * Convert a Janus contact type to a ServiceProvider's ContactPersonType.
   *
   * @param contactType the Janus type
   * @return the {@link ContactPersonType}
   * @throws IllegalArgumentException in case no match can be made.
   */
  public static ContactPersonType contactPersonTypeByJanusContactType(Contact.Type contactType) {
    ContactPersonType t = null;
    if (contactType == Contact.Type.technical) {
      t = ContactPersonType.technical;
    } else if (contactType == Contact.Type.support) {
      t = ContactPersonType.help;
    } else if (contactType == Contact.Type.administrative) {
      t = ContactPersonType.administrative;
    } else if (contactType == Contact.Type.billing) {
      t = ContactPersonType.administrative;
    } else if (contactType == Contact.Type.other) {
      t = ContactPersonType.administrative;
    }
    if (t == null) {
      throw new IllegalArgumentException("Unknown Janus-contactType: " + contactType);
    }
    return t;
  }

  @Override
  public IdentityProvider getIdentityProvider(String idpEntityId) {
    try {
      EntityMetadata metadataByEntityId = janusClient.getMetadataByEntityId(idpEntityId);
      return buildIdentityProviderByMetadata(metadataByEntityId);
    } catch (Exception e) {
      log.error("Unable to getIdentityProvider " + idpEntityId, e);
      return null;
    }
  }

  @Override
  public List<IdentityProvider> getInstituteIdentityProviders(final String instituteId) {
    List<IdentityProvider> idps = new ArrayList<>();
    if (StringUtils.isBlank(instituteId)) {
      return idps;
    }
    // first get all identity providers, then filter the ones we need.
    return this.getAllIdentityProviders().stream().filter(idp -> instituteId.equals(idp.getInstitutionId())).collect(Collectors.toList());
  }

  @Override
  public List<IdentityProvider> getAllIdentityProviders() {
    List<IdentityProvider> idps = new ArrayList<>();
    try {
      final List<EntityMetadata> sps = janusClient.getIdpList();
      for (EntityMetadata metadata : sps) {
        if (StringUtils.equals(metadata.getWorkflowState(), IN_PRODUCTION)) {
          idps.add(buildIdentityProviderByMetadata(metadata));
        }
      }
    } catch (RestClientException e) {
      log.warn("Could not retrieve 'all IdPs' from Janus client", e);
    }
    return idps;
  }

  @Override
  public List<IdentityProvider> getLinkedIdentityProviders(String spId) {
    List<String> allowedIdps = janusClient.getAllowedIdps(spId);
    return this.getAllIdentityProviders().stream().filter(idp -> allowedIdps.contains(idp.getId())).collect(Collectors.toList());
  }

}
