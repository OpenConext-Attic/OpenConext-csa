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
package nl.surfnet.coin.csa.util;

import nl.surfnet.coin.janus.domain.ARP;
import nl.surfnet.coin.janus.domain.Contact;
import nl.surfnet.coin.janus.domain.EntityMetadata;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * JanusRestClientMockTest.java
 */
public class JanusRestClientMockTest {

  private JanusRestClientMock mock = new JanusRestClientMock();
  private final static String SP_ENTITY_ID = "http://mock-sp";
  private final static String IDP_ENTITY_ID = "http://mock-idp";

  private ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

  /**
   * Test method for {@link nl.surfnet.coin.csa.util.JanusRestClientMock#getMetadataByEntityId(java.lang.String)}.
   */
  @Test
  public void testGetMetadataByEntityId() {
    EntityMetadata metaData = mock.getMetadataByEntityId(SP_ENTITY_ID);
    assertEquals(SP_ENTITY_ID, metaData.getAppEntityId());

    metaData = mock.getMetadataByEntityId(IDP_ENTITY_ID);
    assertEquals(IDP_ENTITY_ID, metaData.getAppEntityId());
    assertEquals("mock-institution-id", metaData.getInstutionId());
  }

  @Test
  public void getArp() {
    ARP arp = mock.getArp(SP_ENTITY_ID);
    assertNotNull(arp);
  }

  @Test
  public void generateJson() throws IOException {
    EntityMetadata em = new EntityMetadata();
    em.addName("en", "Populair SP (name en)");
    em.addName("nl", "Populaire SP (name nl)");
    em.addDescription("en", "SP linked to all IdP's (description en)");
    em.addDescription("nl", "SP gelinked aan alle IdP's (description nl)");

    em.setAppDescription("App description");
    em.setAppIcon("http://ipsumimage.appspot.com/320x100?l=App Icon");
    em.setAppThumbNail("http://ipsumimage.appspot.com/320x100?l=App Thumbnail");
    em.setAppTitle("App title");

    em.addAppHomeUrl("en", "https://app_home_url_en");
    em.addAppHomeUrl("nl", "https://app_home_url_nl");
    em.setAppLogoUrl("http://ipsumimage.appspot.com/320x100?l=App Logo");
    em.setEula("https://eula_url");
    em.setInstutionId("institution_id_present");
    em.setApplicationUrl("https://application_url");

    em.addUrl("en", "https://url_en");
    em.addUrl("nl", "https://url_nl");

    em.setIdpVisibleOnly(false);
    em.getContacts().add(createContact(Contact.Type.support));
    em.getContacts().add(createContact(Contact.Type.technical));

    String json = objectMapper.writeValueAsString(em);
    System.out.println(json);
  }

  private Contact createContact(Contact.Type type) {
    Contact contact = new Contact();
    contact.setType(type);
    contact.setEmailAddress(type.name() + "@email.com");
    contact.setGivenName(type.name() + "given_name");
    contact.setSurName(type.name() + "sur_name");
    contact.setTelephoneNumber(type.name() + "telephone_number");
    return contact;
  }


}
