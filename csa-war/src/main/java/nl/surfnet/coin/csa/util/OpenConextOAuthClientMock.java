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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import nl.surfnet.coin.api.client.OAuthVersion;
import nl.surfnet.coin.api.client.OpenConextOAuthClient;
import nl.surfnet.coin.api.client.domain.Email;
import nl.surfnet.coin.api.client.domain.Group;
import nl.surfnet.coin.api.client.domain.Group20;
import nl.surfnet.coin.api.client.domain.Person;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;

import static java.util.Arrays.asList;

/**
 * OpenConextOAuthClientMock.java
 * 
 */
public class OpenConextOAuthClientMock implements OpenConextOAuthClient, InitializingBean {

  public enum Users {
    /*
     * ROLE_DISTRIBUTION_CHANNEL_ADMIN=Distribution Channel Administrator
     */
    ADMIN_DISTRIBUTIE_CHANNEL("admindk"), // admin from surfmarket for csa

    NOT_PERMITTED("NA");

    private String user;

    private Users(String user) {
      this.user = user;
    }

    public String getUser() {
      return user;
    }

    public static Users fromUser(String userName) {
      Users[] values = Users.values();
      for (Users user : values) {
        if (user.getUser().equalsIgnoreCase(userName)) {
          return user;
        }
      }
      return NOT_PERMITTED;
    }
  }

  private String adminDistributionTeam;

  @Override
  public boolean isAccessTokenGranted(String userId) {
    return true;
  }

  @Override
  public String getAuthorizationUrl() {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void oauthCallback(HttpServletRequest request, String onBehalfOf) {
  }

  @Override
  public Person getPerson(String userId, String onBehalfOf) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<Person> getGroupMembers(String groupId, String onBehalfOf) {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
    }
    List<Person> persons = new ArrayList<Person>();
    String group = groupId.substring(groupId.lastIndexOf(":") + 1);
    persons.add(createPerson("John Doe", "john.doe@"+group));
    persons.add(createPerson("Pitje Puck", "p.p@"+group));
    persons.add(createPerson("Yan Yoe", "yan@"+group));
    return persons;
  }

  private Person createPerson(String displayName, String email) {
    Person p = new Person();
    p.setDisplayName(displayName);
    p.setEmails(Collections.singleton(new Email(email)));
    return p;
  }

  @Override
  public List<Group> getGroups(String userId, String onBehalfOf) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<Group20> getGroups20(String userId, String onBehalfOf) {
    final Users user = Users.fromUser(userId);
    switch (user) {
    case ADMIN_DISTRIBUTIE_CHANNEL:
      return asList(createGroup20(adminDistributionTeam));
    default:
      throw new RuntimeException("Unknown");
    }

  }

  private Group20 createGroup20(String id) {
    return new Group20(id, id.substring(id.lastIndexOf(":") + 1), id);
  }

  @Override
  public Group20 getGroup20(String userId, String groupId, String onBehalfOf) {
    throw new RuntimeException("Not implemented");
  }

  /*
   * The following is needed to be conform the contract of the real
   * OpenConextOAuthClient. For the same reason we get the values our selves
   * from the properties files, as we can't inject them
   */

  public void setCallbackUrl(String url) {
  }

  public void setConsumerSecret(String secret) {
  }

  public void setConsumerKey(String key) {
  }

  public void setEndpointBaseUrl(String url) {
  }

  public void setVersion(OAuthVersion v) {
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Properties prop = new Properties();
    prop.load(new ClassPathResource("csa.properties").getInputStream());
    adminDistributionTeam = prop.getProperty("admin.distribution.channel.teamname");
  }

}
