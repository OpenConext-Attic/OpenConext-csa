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

package csa.service;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import csa.domain.CoinUser;
import csa.model.JiraTask;
import csa.service.impl.JiraClientImpl;
import csa.service.impl.deprecated.RemoteIssue;

@RunWith(MockitoJUnitRunner.class)
public class JiraClientImplTest {

  private JiraClientImpl jiraClient;


  @Before
  public void before() {
    jiraClient = new JiraClientImpl("http://foo", "bar", "foobar", "projecKey");
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

    assertThat(captor.getValue().getType(), IsEqual.equalTo(JiraClientImpl.TYPE_QUESTION));
  }

  @Test
  public void getTasks() throws IOException {
    RemoteIssue issue = new RemoteIssue();
    issue.setDescription("bodybody");
    issue.setCustomFieldValues(JiraClientImpl.EMPTY_REMOTE_CUSTOM_FIELD_VALUES);
    issue.setStatus("6");

    final List<JiraTask> tasks = jiraClient.getTasks(Arrays.asList("Foo", "Bar"));
    assertThat(tasks.size(), is(1));
    assertThat(tasks.get(0).getBody(), IsEqual.equalTo("bodybody"));

  }
}
