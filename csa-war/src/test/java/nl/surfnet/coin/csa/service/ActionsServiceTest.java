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

import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.csa.model.JiraTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "/coin-csa-context.xml",
        "/coin-csa-properties-context.xml",
        "classpath:coin-shared-context.xml"})
public class  ActionsServiceTest {

  @Resource(name="actionsService")
  private ActionsService actionsService;

  @Autowired
  private JiraService jiraService;
  
  @Test
  public void synchronization() throws IOException {

    final String idp = "https://mock-idp";

    Action action = new Action(null, "userid", "username", "john.doe@nl", JiraTask.Type.QUESTION, JiraTask.Status.OPEN, "body", idp, "sp", "institute-123", new Date());
    actionsService.registerJiraIssueCreation(action);
    assertEquals("TASK-2", action.getJiraKey());

    action = actionsService.registerAction(action);

    final List<Action> before = actionsService.getActions(idp);

    assertEquals(1, before.size());
    assertEquals(JiraTask.Status.OPEN, before.get(0).getStatus());

    jiraService.doAction(action.getJiraKey(), JiraTask.Action.CLOSE);

    final List<Action> after = actionsService.getActions(idp);

    assertEquals(1, after.size());
    assertEquals(JiraTask.Status.CLOSED, after.get(0).getStatus());
    assertEquals("username", after.get(0).getUserName());
  }

}
