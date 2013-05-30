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

package nl.surfnet.coin.csa.control.shopadmin;

import nl.surfnet.coin.csa.command.LmngServiceBinding;
import nl.surfnet.coin.csa.control.BaseController;
import nl.surfnet.coin.csa.dao.CompoundServiceProviderDao;
import nl.surfnet.coin.csa.dao.LmngIdentifierDao;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.ServiceProvider;
import nl.surfnet.coin.csa.service.CrmService;
import nl.surfnet.coin.csa.service.ServiceProviderService;
import nl.surfnet.coin.csa.service.impl.CompoundSPService;
import nl.surfnet.coin.csa.service.impl.LmngUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/shopadmin/*")
public class SpLnmgListController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(SpLnmgListController.class);

  @Resource(name = "providerService")
  private ServiceProviderService providerService;

  @Resource
  private CrmService licensingService;

  @Autowired
  private LmngIdentifierDao lmngIdentifierDao;

  @Resource
  private CompoundSPService compoundSPService;

  @Autowired
  private CompoundServiceProviderDao compoundServiceProviderDao;

  @RequestMapping(value = "/all-spslmng")
  public ModelAndView listAllSpsLmng(Map<String, Object> model) {
    List<CompoundServiceProvider> services = compoundSPService.getAllCSPs();

    List<LmngServiceBinding> lmngServiceBindings = new ArrayList<LmngServiceBinding>();
    for (ServiceProvider serviceProvider : providerService.getAllServiceProviders(false)) {
      String lmngIdentifier = lmngIdentifierDao.getLmngIdForServiceProviderId(serviceProvider.getId());
      CompoundServiceProvider compoundServiceProvider = compoundServiceProviderDao.findByEntityId(serviceProvider.getId());
      if (compoundServiceProvider != null) {
        lmngServiceBindings.add(new LmngServiceBinding(lmngIdentifier, serviceProvider, compoundServiceProvider));
      }
    }

    model.put("bindings", lmngServiceBindings);
    return new ModelAndView("shopadmin/sp-overview", model);
  }

  @RequestMapping(value = "/save-splmng", method = RequestMethod.POST)
  public ModelAndView saveLmngServices(HttpServletRequest req) {
    Map<String, Object> model = new HashMap<String, Object>();

    String spId = req.getParameter("spIdentifier");
    String lmngId = req.getParameter("lmngIdentifier");
    Integer index = Integer.valueOf(req.getParameter("index"));

    String isClearPressed = req.getParameter("clearbutton");
    if (StringUtils.isNotBlank(isClearPressed)) {
      log.debug("Clearing lmng identifier for ServiceProvider with ID " + spId);
      lmngId = null;
    } else {
      // extra validation (also done in frontend/jquery)
      if (!LmngUtil.isValidGuid(lmngId)) {
        model.put("errorMessage", "jsp.lmng_binding_overview.wrong.guid");
        model.put("messageIndex", index);
        return listAllSpsLmng(model);
      }

      String serviceLmngName = licensingService.getServiceName(lmngId);
      if (serviceLmngName == null) {
        model.put("errorMessage", "jsp.lmng_binding_overview.unknown.guid");
        model.put("messageIndex", index);
      } else {
        model.put("infoMessage", serviceLmngName);
        model.put("messageIndex", index);
      }
      log.debug("Storing lmng identifier " + lmngId + " for ServiceProvider with ID " + spId);
    }
    lmngIdentifierDao.saveOrUpdateLmngIdForServiceProviderId(spId, lmngId);

    return listAllSpsLmng(model);
  }

  @RequestMapping(value = "/update-csp-public-api/{cspId}/{newValue}", method = RequestMethod.PUT)
  public
  @ResponseBody
  String updateCspPublicApi(@PathVariable("cspId") Long cspId, @PathVariable("newValue") boolean newValue) {
    CompoundServiceProvider csp = compoundServiceProviderDao.findById(cspId);
    csp.setHideInPublicCsa(newValue);
    compoundServiceProviderDao.saveOrUpdate(csp);
    log.info("Updated CompoundServiceProvider(" + cspId + ") to be included in public API:" + newValue);
    return "ok";
  }

  @RequestMapping(value = "/update-csp-protected-api/{cspId}/{newValue}", method = RequestMethod.PUT)
  public
  @ResponseBody
  String updateCspProtectedApi(@PathVariable("cspId") Long cspId, @PathVariable("newValue") boolean newValue) {
    CompoundServiceProvider csp = compoundServiceProviderDao.findById(cspId);
    csp.setHideInProtectedCsa(newValue);
    compoundServiceProviderDao.saveOrUpdate(csp);
    log.info("Updated CompoundServiceProvider(" + cspId + ") to be included in protected API:" + newValue);
    return "ok";
  }

}
