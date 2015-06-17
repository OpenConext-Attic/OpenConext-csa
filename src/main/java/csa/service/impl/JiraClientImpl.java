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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import csa.domain.CoinUser;
import csa.model.JiraTask;
import csa.service.impl.deprecated.RemoteCustomFieldValue;
import csa.service.impl.deprecated.RemoteIssue;

public class JiraClientImpl implements JiraClient {
  private static final Logger LOG = LoggerFactory.getLogger(JiraClientImpl.class);

  public static final String[] EMPTY_STRINGS = new String[0];
  public static final RemoteCustomFieldValue[] EMPTY_REMOTE_CUSTOM_FIELD_VALUES = new RemoteCustomFieldValue[0];
  public static final String SP_CUSTOM_FIELD = "customfield_10100";
  public static final String IDP_CUSTOM_FIELD = "customfield_10101";
  private static final long DEFAULT_SECURITY_LEVEL = 10100L;

  public static final String TYPE_LINKREQUEST = "13";
  public static final String TYPE_UNLINKREQUEST = "17";
  public static final String TYPE_QUESTION = "16";

  private static final Map<JiraTask.Type, String> TASKTYPE_TO_ISSUETYPE_CODE = ImmutableMap.of(
    JiraTask.Type.QUESTION, "16",
    JiraTask.Type.LINKREQUEST, "13",
    JiraTask.Type.UNLINKREQUEST, "17");

  public static final String PRIORITY_MEDIUM = "3";

  private final String baseUrl;

  private final RestTemplate restTemplate;
  private final String projectKey;
  private final HttpHeaders defaultHeaders;


  public JiraClientImpl(final String baseUrl, final String username, final String password, final String projectKey) {
    this.projectKey = projectKey;
    this.baseUrl = baseUrl;

    defaultHeaders = new HttpHeaders();
    defaultHeaders.setContentType(MediaType.APPLICATION_JSON);
    final byte[] encoded = Base64.encode((username + ":" + password).getBytes());
    defaultHeaders.add("Authorization", "Basic " + new String(encoded));
    this.restTemplate = new RestTemplate();
  }

  @Override
  public String create(final JiraTask task, CoinUser user) {
    RemoteIssue remoteIssue;
    switch (task.getIssueType()) {
      case LINKREQUEST:
      case UNLINKREQUEST:
        remoteIssue = createRequest(task, user);
        break;
      default:
        remoteIssue = createQuestion(task, user);
        break;
    }
    //TODO hans call REST api here with DEFAULT_SECURITY_LEVEL
    // jiraSoapService.createIssueWithSecurityLevel(getToken(), remoteIssue, DEFAULT_SECURITY_LEVEL);
    final RemoteIssue createdIssue = null;


    if (createdIssue == null) {
      return null;
    }
    return createdIssue.getKey();
  }

  private RemoteIssue createQuestion(final JiraTask task, CoinUser user) {
    RemoteIssue remoteIssue = new RemoteIssue();
    remoteIssue.setType(TYPE_QUESTION);
    remoteIssue.setSummary(new StringBuilder().append("Question about ").append(task.getServiceProvider()).toString());
    remoteIssue.setProject(projectKey);
    remoteIssue.setPriority(PRIORITY_MEDIUM);
    StringBuilder description = new StringBuilder();
    description.append("Applicant name: ").append(user.getDisplayName()).append("\n");
    description.append("Applicant email: ").append(user.getEmail()).append("\n");
    description.append("Identity Provider: ").append(task.getIdentityProvider()).append("\n");
    description.append("Service Provider: ").append(task.getServiceProvider()).append("\n");
    description.append("Time: ").append(new SimpleDateFormat("HH:mm dd-MM-yy").format(new Date())).append("\n");
    description.append("Service Provider: ").append(task.getServiceProvider()).append("\n");
    description.append("Request: ").append(task.getBody()).append("\n");
    remoteIssue.setDescription(description.toString());
    appendSpAndIdp(task, remoteIssue);
    return remoteIssue;
  }

  private RemoteIssue createRequest(final JiraTask task, CoinUser user) {
    RemoteIssue remoteIssue = new RemoteIssue();
    remoteIssue.setType(TASKTYPE_TO_ISSUETYPE_CODE.get(task.getIssueType()));
    if (remoteIssue.getType().equals(TYPE_LINKREQUEST)) {
      remoteIssue.setSummary("New connection for IdP " + task.getIdentityProvider() + " to SP " + task.getServiceProvider());
    } else if (remoteIssue.getType().equals(TYPE_UNLINKREQUEST)) {
      remoteIssue.setSummary("Disconnect IdP " + task.getIdentityProvider() + " and SP " + task.getServiceProvider());
    } else {
      throw new IllegalStateException("Unknown type: " + remoteIssue.getType());
    }

    remoteIssue.setProject(projectKey);
    remoteIssue.setPriority(PRIORITY_MEDIUM);
    StringBuilder description = new StringBuilder();
    if (task.getIssueType() == JiraTask.Type.LINKREQUEST) {
      description.append("Request: Create a new connection").append("\n");
    } else {
      description.append("Request: terminate a connection").append("\n");
    }

    description.append("Applicant name: ").append(user.getDisplayName()).append("\n");
    description.append("Applicant email: ").append(user.getEmail()).append("\n");
    description.append("Identity Provider: ").append(task.getIdentityProvider()).append("\n");
    description.append("Service Provider: ").append(task.getServiceProvider()).append("\n");
    description.append("Time: ").append(new SimpleDateFormat("HH:mm dd-MM-yy").format(new Date())).append("\n");
    description.append("Service Provider: ").append(task.getServiceProvider()).append("\n");
    description.append("Remark from user: ").append(task.getBody()).append("\n");
    remoteIssue.setDescription(description.toString());
    appendSpAndIdp(task, remoteIssue);
    return remoteIssue;
  }

  private void appendSpAndIdp(final JiraTask task, final RemoteIssue remoteIssue) {
    final List<RemoteCustomFieldValue> customFieldValues = new ArrayList<>();
    final List<String> spValue = Collections.singletonList(task.getServiceProvider());
    final List<String> idpValue = Collections.singletonList(task.getIdentityProvider());
    customFieldValues.add(new RemoteCustomFieldValue(SP_CUSTOM_FIELD, null, spValue.toArray(EMPTY_STRINGS)));
    customFieldValues.add(new RemoteCustomFieldValue(IDP_CUSTOM_FIELD, null, idpValue.toArray(EMPTY_STRINGS)));
    remoteIssue.setCustomFieldValues(customFieldValues.toArray(EMPTY_REMOTE_CUSTOM_FIELD_VALUES));
  }

  @Override
  public List<JiraTask> getTasks(final List<String> keys) {
    if (keys == null || keys.size() == 0) {
      return Collections.emptyList();
    }
    StringBuilder query = new StringBuilder("project = ");
    query.append(projectKey);
    query.append(" AND key IN (");
    Joiner.on(",").skipNulls().appendTo(query, keys);
    query.append(")");
    if (LOG.isDebugEnabled()) {
      LOG.debug("Sending query to JIRA: " + query.toString());
    }
    final Map<String, String> searchArgs = ImmutableMap.of("jql", query.toString());

    try {
      HttpEntity<Map<String, String>> entity = new HttpEntity<>(searchArgs, defaultHeaders);
      Map<String, Object> result = restTemplate.postForObject(baseUrl + "/search?expand=all", entity, Map.class);

      List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
      return issues.stream().
        map(issue -> {
          final String key = (String) issue.get("key");

          final Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
          final Optional<String> identityProvider = Optional.ofNullable((String) fields.get(IDP_CUSTOM_FIELD));
          final Optional<String> serviceProvider = Optional.ofNullable((String) fields.get(SP_CUSTOM_FIELD));
          final String description = (String) fields.get("description");

          final Map<String, Object> statusInfo = (Map<String, Object>) fields.get("status");
          final JiraTask.Status status = JiraTask.Status.valueOf(((String) statusInfo.get("name")).toUpperCase());
          return new JiraTask.Builder().key(key).identityProvider(identityProvider.orElse(""))
            .serviceProvider(serviceProvider.orElse("")).institution("???").status(status).body(description).build();
        }).
        collect(Collectors.toList());
    } catch (RestClientException e) {
      LOG.error("Error communicating with Jira, return empty list", e);
      return Collections.emptyList();
    }
  }
}
