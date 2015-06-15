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

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import csa.dao.impl.ActionsDaoImpl;
import csa.domain.CoinUser;
import csa.model.Action;
import csa.model.JiraTask;
import csa.service.ActionsService;
import csa.service.JiraClient;

@Service(value = "actionsService")
public class ActionsServiceImpl implements ActionsService {

  private static final Logger LOG = LoggerFactory.getLogger(ActionsServiceImpl.class);

  @Autowired
  private ActionsDaoImpl actionsDao;

  @Autowired
  private JiraClient jiraClient;

  @Override
  public List<Action> getActions(String identityProvider) {
    try {
      synchronizeWithJira(identityProvider);
    } catch (IOException e) {
      LOG.error("Could not synchronize with JIRA", e);
    }
    return actionsDao.findActionsByIdP(identityProvider);
  }

  @Override
  public void registerJiraIssueCreation(Action action) {
    JiraTask task = new JiraTask.Builder()
      .body(action.getUserEmail() + ("\n\n" + action.getBody()))
      .identityProvider(action.getIdpId()).serviceProvider(action.getSpId())
      .institution(action.getInstitutionId()).issueType(action.getType())
      .status(JiraTask.Status.OPEN).build();
    try {
      String jiraKey = jiraClient.create(task, createUser(action));
      action.setJiraKey(jiraKey);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Action registerAction(Action action) {
    actionsDao.saveAction(action);
    return action;
  }

  private CoinUser createUser(Action action) {
    CoinUser coinUser = new CoinUser();
    coinUser.setDisplayName(action.getUserName());
    coinUser.setEmail(action.getUserEmail());
    coinUser.setUid(action.getUserId());
    return coinUser;
  }

  private void synchronizeWithJira(String identityProvider) throws IOException {
    List<String> openTasks = actionsDao.getKeys(identityProvider);
    final List<JiraTask> tasks = jiraClient.getTasks(openTasks);
    for (JiraTask task : tasks) {
      if (task.getStatus() == JiraTask.Status.CLOSED) {
        actionsDao.close(task.getKey());
      }
    }
  }
}
