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
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.selfservice.interceptor.AuthorityScopeInterceptor;
import nl.surfnet.coin.selfservice.service.ActionsService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping
public class HistoryController extends BaseApiController {

  @Resource(name = "actionsService")
  private ActionsService actionsService;

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/history.json")
  public @ResponseBody
  List<Action> listActions(HttpServletRequest request) throws IOException {
    verifyScope(request, AuthorityScopeInterceptor.DASHBOARD_OAUTH_CLIENT_SCOPE);
    String idp = getIdpEntityIdFromToken(request);
    actionsService.synchronizeWithJira(idp);
    return actionsService.getActions(idp);
  }

}
