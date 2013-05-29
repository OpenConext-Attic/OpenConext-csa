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

package nl.surfnet.coin.selfservice.service.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import nl.surfnet.coin.selfservice.domain.CoinUser;
import org.springframework.stereotype.Service;

import nl.surfnet.coin.selfservice.dao.impl.ActionsDaoImpl;
import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.csa.model.JiraTask;
import nl.surfnet.coin.selfservice.service.ActionsService;
import nl.surfnet.coin.selfservice.service.JiraService;
import nl.surfnet.coin.selfservice.service.ServiceProviderService;

@Service(value = "actionsService")
public class ActionsServiceImpl implements ActionsService {

  @Resource(name="actionsDao")
  private ActionsDaoImpl actionsDao;

  @Resource(name="jiraService")
  private JiraService jiraService;


  @Resource(name="providerService")
  private ServiceProviderService providerService;

  @Override
  public List<Action> getActions(String identityProvider) {
    return actionsDao.findActionsByIdP(identityProvider);
  }

  @Override
  public Action registerJiraIssueCreation(Action action) {
    JiraTask task = new JiraTask.Builder()
            .body(action.getUserEmail() + ("\n\n" + action.getBody()))
            .identityProvider(action.getIdpId()).serviceProvider(action.getSpId())
            .institution(action.getInstitutionId()).issueType(action.getType())
            .status(JiraTask.Status.OPEN).build();
    String issueKey = getJiraKey(action, task);
    action.setJiraKey(issueKey);
    action.setStatus(JiraTask.Status.OPEN);
    actionsDao.saveAction(action);
    return action;
  }

  private String getJiraKey(Action action, JiraTask task) {
    String issueKey;
    try {
      issueKey = jiraService.create(task, createUser(action));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return issueKey;
  }

  private CoinUser createUser(Action action) {
    CoinUser coinUser = new CoinUser();
    coinUser.setDisplayName(action.getUserName());
    coinUser.setEmail(action.getUserEmail());
    coinUser.setUid(action.getUserId());
    return coinUser;
  }

  @Override
  public void synchronizeWithJira(String identityProvider) throws IOException {
    List<String> openTasks = actionsDao.getKeys(identityProvider);
    final List<JiraTask> tasks = jiraService.getTasks(openTasks);
    for (JiraTask task : tasks) {
      if (task.getStatus() == JiraTask.Status.CLOSED) {
        actionsDao.close(task.getKey());
      }
    }
  }
}
