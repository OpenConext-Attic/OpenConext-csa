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

package nl.surfnet.coin.csa.model;

import java.util.Comparator;
import java.util.Date;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;



public class Action {


  public enum Type {
    QUESTION, LINKREQUEST, UNLINKREQUEST;

    public static Type byJiraIssueType(JiraTask.Type type) {
      switch (type) {
        case QUESTION:
          return QUESTION;
        case LINKREQUEST:
          return LINKREQUEST;
        case UNLINKREQUEST:
          return UNLINKREQUEST;
        default:
          throw new IllegalStateException("Unknown jira issue type: " + type);
      }
    }
  }

  public enum Status {
    OPEN, CLOSED;

    public static Status byJiraIssueStatus(JiraTask.Status status) {
      switch (status) {
        case OPEN:
          return OPEN;
        case CLOSED:
          return CLOSED;
        default:
          throw new IllegalStateException("Unknown jira issue status: " + status);
      }
    }
  }

  private long id;
  private String jiraKey;
  private String userId;
  private String userName;
  private String body;
  private String idpId;
  private String spId;
  private Date requestDate;
  private Type type;
  private Status status;
  private String institutionId;

  public Action(String jiraKey, String userId, String userName, Type type, Status status, String body, String idpId,
                String spId, String institutionId, Date requestDate) {
    this.userId = userId;
    this.jiraKey = jiraKey;
    this.userName = userName;
    this.body = body;
    this.idpId = idpId;
    this.spId = spId;
    this.requestDate = requestDate;
    this.type = type;
    this.status = status;
    this.institutionId = institutionId;
  }

  public String getJiraKey() {
    return jiraKey;
  }

  public String getUserName() {
    return userName;
  }

  public String getUserId() {
    return userId;
  }

  public String getBody() {
    return body;
  }

  public String getIdpId() {
    return idpId;
  }

  public String getSpId() {
    return spId;
  }

  public Date getRequestDate() {
    return requestDate;
  }

  public Type getType() {
    return type;
  }

  public Status getStatus() {
    return status;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }
  public String getInstitutionId() {
    return institutionId;
  }


  /**
   * get a Comparator that sorts by date ascending: newest first
   * @return
   */
  public static Comparator<? super Action> sortByDateAsc() {
    return new Comparator<Action>() {
      @Override
      public int compare(Action a1, Action a2) {
        return new CompareToBuilder()
            .append(a1.getRequestDate(), a2.getRequestDate())
            .toComparison();
      }
    };
  }


}
