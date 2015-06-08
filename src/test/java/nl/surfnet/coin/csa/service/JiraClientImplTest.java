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

package nl.surfnet.coin.csa.service;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.model.JiraTask;
import nl.surfnet.coin.csa.service.impl.JiraClientImpl;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.swift.common.soap.jira.JiraSoapService;
import org.swift.common.soap.jira.RemoteIssue;

@RunWith(MockitoJUnitRunner.class)
public class JiraClientImplTest {

  private JiraClientImpl jiraClient;

  private JiraSoapService jss;

  @Before
  public void before() {
    jss = Mockito.mock(JiraSoapService.class);
    jiraClient = new JiraClientImpl(jss, "bar", "foobar", "projecKey");
  }

  @Test
  public void createRequest() throws IOException {
    ArgumentCaptor<RemoteIssue> captor = ArgumentCaptor.forClass(RemoteIssue.class);

    final JiraTask task = new JiraTask.Builder()
        .body("thebody")
        .identityProvider("idp")
        .serviceProvider("sp")
        .issueType(JiraTask.Type.UNLINKREQUEST)
        .build();
    jiraClient.create(task, new CoinUser());
    verify(jss).createIssueWithSecurityLevel(anyString(), captor.capture(), eq(10100L));

    assertThat("Given body should be set as the issue's Description field",
        captor.getValue().getDescription(), containsString("thebody"));

    assertThat(captor.getValue().getType(), IsEqual.equalTo(JiraClientImpl.TYPE_UNLINKREQUEST));
  }

  @Test
  public void createQuestion() throws IOException {
    ArgumentCaptor<RemoteIssue> captor = ArgumentCaptor.forClass(RemoteIssue.class);

    final JiraTask task = new JiraTask.Builder()
        .body("thebody")
        .identityProvider("idp")
        .serviceProvider("sp")
        .issueType(JiraTask.Type.QUESTION)
        .build();
    jiraClient.create(task, new CoinUser());
    verify(jss).createIssueWithSecurityLevel(anyString(), captor.capture(),eq(10100L));

    assertThat(captor.getValue().getType(), IsEqual.equalTo(JiraClientImpl.TYPE_QUESTION));
  }

  @Test
  public void getTasks() throws IOException {
    RemoteIssue issue = new RemoteIssue();
    issue.setDescription("bodybody");
    issue.setCustomFieldValues(JiraClientImpl.EMPTY_REMOTE_CUSTOM_FIELD_VALUES);
    issue.setStatus("6");
    when(jss.getIssuesFromJqlSearch(anyString(), anyString(), anyInt())).thenReturn(
        new RemoteIssue[]{
            issue});
    final List<JiraTask> tasks = jiraClient.getTasks(Arrays.asList("Foo", "Bar"));
    assertThat(tasks.size(), is(1));
    assertThat(tasks.get(0).getBody(), IsEqual.equalTo("bodybody"));

  }
}
