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

package nl.surfnet.coin.csa.service.impl;

import nl.surfnet.coin.csa.domain.ContactPerson;
import nl.surfnet.coin.csa.domain.ContactPersonType;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.domain.ServiceProvider;
import nl.surfnet.coin.csa.service.IdentityProviderService;
import nl.surfnet.coin.csa.service.ServiceProviderService;
import nl.surfnet.coin.janus.Janus;
import nl.surfnet.coin.janus.domain.ARP;
import nl.surfnet.coin.janus.domain.Contact;
import nl.surfnet.coin.janus.domain.EntityMetadata;
import nl.surfnet.coin.janus.domain.JanusEntity;
import nl.surfnet.coin.shared.domain.ErrorMail;
import nl.surfnet.coin.shared.service.ErrorMessageMailer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistryProviderService implements ServiceProviderService, IdentityProviderService {

  private static final Logger log = LoggerFactory.getLogger(ServiceRegistryProviderService.class);
  private static final String IN_PRODUCTION = "prodaccepted";

  @Resource(name = "janusClient")
  private Janus janusClient;

  @Resource(name = "errorMessageMailer")
  private ErrorMessageMailer errorMessageMailer;

  @Override
  public List<ServiceProvider> getAllServiceProviders(String idpId) {
    List<ServiceProvider> allSPs = getAllServiceProvidersUnfiltered();

    List<String> myLinkedSPs = getLinkedServiceProviderIDs(idpId);

    List<ServiceProvider> filteredList = new ArrayList<ServiceProvider>();
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
  public List<ServiceProvider> getAllServiceProviders() {
    return getAllServiceProvidersUnfiltered();
  }

  @Override
  public List<String> getLinkedServiceProviderIDs(String idpId) {
    List<String> spList = new ArrayList<String>();
    try {
      spList = janusClient.getAllowedSps(idpId);
    } catch (RestClientException e) {
      log.warn("Could not retrieve allowed SPs from Janus client", e.getMessage());
      sendErrorMail("RestClientException", e.getMessage(), "LinkedServiceProviderIDs");
    }
    return spList;
  }

  private List<ServiceProvider> getAllServiceProvidersUnfiltered() {
    List<ServiceProvider> spList = new ArrayList<ServiceProvider>();
    try {
      final List<EntityMetadata> sps = janusClient.getSpList();
      for (EntityMetadata metadata : sps) {
        if (StringUtils.equals(metadata.getWorkflowState(), IN_PRODUCTION)) {
          spList.add(buildServiceProviderByMetadata(metadata));
        }
      }
    } catch (RestClientException e) {
      log.warn("Could not retrieve 'all SPs' from Janus client", e.getMessage());
      sendErrorMail("RestClientException", e.getMessage(), "AllServiceProvidersUnfiltered");
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
      final ServiceProvider serviceProvider = buildServiceProviderByMetadata(metadata);

      // Check if the IdP can connect to this service
      if (idpEntityId != null) {
        final boolean linked = janusClient.isConnectionAllowed(spEntityId, idpEntityId);
        serviceProvider.setLinked(linked);
      }

      return serviceProvider;
    } catch (RestClientException e) {
      log.warn("Could not retrieve metadata from Janus client", e.getMessage());
      sendErrorMail("RestClientException", e.getMessage(), "ServiceProvider");
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
   * @param metadata Janus metadata
   * @return {@link ServiceProvider}
   */
  public ServiceProvider buildServiceProviderByMetadata(EntityMetadata metadata) {
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
    for (Contact c : metadata.getContacts()) {
      ContactPerson p = new ContactPerson(StringUtils.join(new Object[]{c.getGivenName(), c.getSurName()}, " "), c.getEmailAddress());
      p.setContactPersonType(contactPersonTypeByJanusContactType(c.getType()));
      p.setTelephoneNumber(c.getTelephoneNumber());
      sp.addContactPerson(p);
    }
    // Get the ARP (if there is any)
    final ARP arp = janusClient.getArp(appEntityId);
    sp.setArp(arp);

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
      sendErrorMail("Exception", e.getMessage(), "identityProvider");
      return null;
    }
  }

  @Override
  public List<IdentityProvider> getInstituteIdentityProviders(String instituteId) {
    List<IdentityProvider> idps = new ArrayList<IdentityProvider>();
    if (StringUtils.isBlank(instituteId)) {
      return idps;
    }
    // first get all entities id's
    List<String> entityIds = janusClient.getEntityIdsByMetaData(Janus.Metadata.INSITUTION_ID, instituteId);
    for (String entityId : entityIds) {
      idps.add(getIdentityProvider(entityId));
    }
    return idps;
  }

  @Override
  public List<IdentityProvider> getAllIdentityProviders() {
    List<IdentityProvider> idps = new ArrayList<IdentityProvider>();
    try {
      final List<EntityMetadata> sps = janusClient.getIdpList();
      for (EntityMetadata metadata : sps) {
        if (StringUtils.equals(metadata.getWorkflowState(), IN_PRODUCTION)) {
          idps.add(buildIdentityProviderByMetadata(metadata));
        }
      }
    } catch (RestClientException e) {
      log.warn("Could not retrieve 'all IdPs' from Janus client", e.getMessage());
    }
    return idps;
  }

  /*
   * Send a mail
   */
  private void sendErrorMail(String exceptionName, String message, String method) {
    String shortMessage = exceptionName + " while retrieving information from Janus";
    String formattedMessage = String.format("Janus call failed with a " + exceptionName + " containing the following message: '%s'",
            message);
    ErrorMail errorMail = new ErrorMail(shortMessage, formattedMessage, formattedMessage, getHost(), "Janus");
    errorMail.setLocation(this.getClass().getName() + "#get" + method);
    errorMessageMailer.sendErrorMail(errorMail);
  }

  private String getHost() {
    try {
      return InetAddress.getLocalHost().toString();
    } catch (UnknownHostException e) {
      return "UNKNOWN";
    }
  }

}
