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

package nl.surfnet.coin.csa.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Service implements Comparable<Service> {

  private long id;

  private String name;

  private String description;

  private String logoUrl;

  private String websiteUrl;

  private String appUrl;

  private String serviceUrl;

  private String crmUrl;

  private String detailLogoUrl;

  private String supportUrl;

  private String eulaUrl;

  private List<String> screenshotUrls;

  private String supportMail;

  private String enduserDescription;

  private String institutionDescription;

  /**
   * Whether this service is connected to the IdP in the service registry
   */
  private boolean connected;

  /**
   * Whether this service is connected to an item in the CRM
   */
  private boolean hasCrmLink;

  /**
   * The article in the CRM this service is linked to. If set, hasCrmLink is true;
   */
  private CrmArticle crmArticle;

  /**
   * The license from the CRM. If set, hasCrmLink is true
   */
  private License license;

  private Map<Category, List<CategoryValue>> categories;

  private String spEntityId;

  private Map<String, List<String>> arp;

  public Service() {
  }

  public Service(long id, String name, String logoUrl, String websiteUrl, boolean hasCrmLink, String crmUrl) {
    this.id = id;
    this.name = name;
    this.logoUrl = logoUrl;
    this.websiteUrl = websiteUrl;
    this.hasCrmLink = hasCrmLink;
    this.crmUrl = crmUrl;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public String getWebsiteUrl() {
    return websiteUrl;
  }

  public void setWebsiteUrl(String websiteUrl) {
    this.websiteUrl = websiteUrl;
  }

  public boolean isHasCrmLink() {
    return hasCrmLink;
  }

  public void setHasCrmLink(boolean hasCrmLink) {
    this.hasCrmLink = hasCrmLink;
  }

  public String getCrmUrl() {
    return crmUrl;
  }

  public void setCrmUrl(String crmUrl) {
    this.crmUrl = crmUrl;
  }

  public Map<Category, List<CategoryValue>> getCategories() {
    return categories;
  }

  public void setCategories(Map<Category, List<CategoryValue>> categories) {
    this.categories = categories;
  }

  @Override
  public int compareTo(Service other) {
    if (other == null) {
      return 1;
    }
    String otherName =  other.getName();
    if (this.name == null && otherName == null ) {
      return -1;
    }
    if (this.name == null) {
      return -1;
    }
    if (otherName == null) {
      return 1;
    }
    return this.name.compareTo(otherName);
  }

  public boolean isConnected() {
    return connected;
  }

  public void setConnected(boolean connected) {
    this.connected = connected;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public License getLicense() {
    return license;
  }

  public void setLicense(License license) {
    this.license = license;
  }

  @JsonIgnore
  public String getSearchFacetValues() {
    Collection<String> values = new ArrayList<String>();
    if (categories != null && !categories.isEmpty()) {
      for (List<CategoryValue> c : categories.values()) {
        for (CategoryValue value : c) {
          values.add(value.getSearchValue());
        }
      }
    }
    return StringUtils.join(values, " ");
  }

  public String getAppUrl() {
    return appUrl;
  }

  public void setAppUrl(String appUrl) {
    this.appUrl = appUrl;
  }

  public String getSpEntityId() {
    return spEntityId;
  }

  public void setSpEntityId(String spEntityId) {
    this.spEntityId = spEntityId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getServiceUrl() {
    return serviceUrl;
  }

  public void setServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  public String getDetailLogoUrl() {
    return detailLogoUrl;
  }

  public void setDetailLogoUrl(String detailLogoUrl) {
    this.detailLogoUrl = detailLogoUrl;
  }

  public CrmArticle getCrmArticle() {
    return crmArticle;
  }

  public void setCrmArticle(CrmArticle crmArticle) {
    this.crmArticle = crmArticle;
  }

  public String getSupportUrl() {
    return supportUrl;
  }

  public void setSupportUrl(String supportUrl) {
    this.supportUrl = supportUrl;
  }

  public String getEulaUrl() {
    return eulaUrl;
  }

  public void setEulaUrl(String eulaUrl) {
    this.eulaUrl = eulaUrl;
  }

  public List<String> getScreenshotUrls() {
    return screenshotUrls;
  }

  public void setScreenshotUrls(List<String> screenshotUrls) {
    this.screenshotUrls = screenshotUrls;
  }

  public String getSupportMail() {
    return supportMail;
  }

  public void setSupportMail(String supportMail) {
    this.supportMail = supportMail;
  }

  public String getEnduserDescription() {
    return enduserDescription;
  }

  public void setEnduserDescription(String enduserDescription) {
    this.enduserDescription = enduserDescription;
  }

  public String getInstitutionDescription() {
    return institutionDescription;
  }

  public void setInstitutionDescription(String institutionDescription) {
    this.institutionDescription = institutionDescription;
  }

  public Map<String, List<String>> getArp() {
    return arp;
  }

  public void setArp(Map<String, List<String>> arp) {
    this.arp = arp;
  }
}
