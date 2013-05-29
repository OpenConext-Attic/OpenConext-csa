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

package nl.surfnet.coin.selfservice.api.control;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.csa.model.JiraTask;
import nl.surfnet.coin.selfservice.domain.CoinUser;
import nl.surfnet.coin.selfservice.domain.IdentityProvider;
import nl.surfnet.coin.selfservice.domain.ServiceProvider;
import nl.surfnet.coin.selfservice.interceptor.AuthorityScopeInterceptor;
import nl.surfnet.coin.selfservice.service.*;

import nl.surfnet.coin.selfservice.util.SpringSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping
public class JiraController extends BaseApiController {


  private static final Logger LOG = LoggerFactory.getLogger(JiraController.class);

  @Resource(name = "actionsService")
  private ActionsService actionsService;

  @Resource(name = "emailService")
  private EmailService emailService;

  @Resource
  private ServiceProviderService serviceProviderService;

  @Resource
  private IdentityProviderService identityProviderService;

  @Value("${administration.email.enabled:true}")
  private boolean sendAdministrationEmail;

  @Value("${administration.jira.ticket.enabled:false}")
  private boolean createAdministrationJiraTicket;

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/actions.json")
  public @ResponseBody
  List<Action> listActions(HttpServletRequest request) throws IOException {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_JIRA);
    String idp = getIdpEntityIdFromToken(request);
    return actionsService.getActions(idp);
  }

  @RequestMapping(value = "/api/protected/action.json", method = RequestMethod.POST)
  public
  @ResponseBody
  Action newAction(HttpServletRequest request, @RequestBody Action action) throws IOException {
    verifyScope(request, AuthorityScopeInterceptor.OAUTH_CLIENT_SCOPE_JIRA);

    ServiceProvider serviceProvider = serviceProviderService.getServiceProvider(action.getSpId());
    IdentityProvider identityProvider = identityProviderService.getIdentityProvider(action.getIdpId());
    String issueKey = null;
    if (createAdministrationJiraTicket) {
        action = actionsService.registerJiraIssueCreation(action);
    }
    if (sendAdministrationEmail) {
      sendAdministrationEmail(serviceProvider, identityProvider, issueKey, action);
    }
  return action ;
  }

  private void sendAdministrationEmail(ServiceProvider serviceProvider,IdentityProvider identityProvider , String issueKey, Action action) {
    String subject = String.format("[Dashboard (" + getHost() + ") request] %s connection from IdP '%s' to SP '%s' (Issue : %s)",
            action.getType().name(), action.getIdpId(), action.getSpId(), issueKey);

    StringBuilder body = new StringBuilder();
    body.append("Domain of Reporter: " + action.getInstitutionId() + "\n");
    body.append("SP EntityID: " + serviceProvider.getId() + "\n");
    body.append("SP Name: " + serviceProvider.getName() + "\n");

    body.append("IdP EntityID: " + identityProvider.getId() + "\n");
    body.append("IdP Name: " + identityProvider.getName() + "\n");


    body.append("Request: " + action.getType().name() + "\n");
    body.append("Applicant name: " + action.getUserName() + "\n");
    body.append("Applicant email: " + action.getUserEmail() + " \n");
    body.append("Mail applicant: mailto:" + action.getUserEmail()+"?CC=surfconext-beheer@surfnet.nl&SUBJECT=["+issueKey+"]%20"+action.getType().name()+"%20to%20"+serviceProvider.getName()+"&BODY=Beste%20" + action.getUserName() + " \n");

    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:MM");
    body.append("Time: " + sdf.format(new Date()) + "\n");
    body.append("Remark from User:\n");
    body.append(action.getBody());
    emailService.sendMail(action.getUserEmail(), subject.toString(), body.toString());
  }

  private String getHost() {
    try {
      return InetAddress.getLocalHost().toString();
    } catch (UnknownHostException e) {
      return "UNKNOWN";
    }
  }


}
