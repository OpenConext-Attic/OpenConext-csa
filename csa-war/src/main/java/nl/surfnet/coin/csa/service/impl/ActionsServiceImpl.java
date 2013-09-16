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

package nl.surfnet.coin.csa.service.impl;

import nl.surfnet.coin.csa.dao.impl.ActionsDaoImpl;
import nl.surfnet.coin.csa.domain.CoinUser;
import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.csa.model.JiraTask;
import nl.surfnet.coin.csa.service.ActionsService;
import nl.surfnet.coin.csa.service.JiraService;
import nl.surfnet.coin.csa.service.ServiceProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service(value = "actionsService")
public class ActionsServiceImpl implements ActionsService {

  private static final Logger LOG = LoggerFactory.getLogger(ActionsServiceImpl.class);

  @Resource(name="actionsDao")
  private ActionsDaoImpl actionsDao;

  @Resource(name="jiraService")
  private JiraService jiraService;


  @Resource(name="providerService")
  private ServiceProviderService providerService;

  @Override
  public List<Action> getActions(String identityProvider) {
    try {
      synchronizeWithJira(identityProvider);
    } catch (IOException e) {
      //tough luck
      LOG.warn("Could not synchronize with JIRA", e);
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
      String jiraKey =  jiraService.create(task, createUser(action));
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
    final List<JiraTask> tasks = jiraService.getTasks(openTasks);
    for (JiraTask task : tasks) {
      if (task.getStatus() == JiraTask.Status.CLOSED) {
        actionsDao.close(task.getKey());
      }
    }
  }
}
