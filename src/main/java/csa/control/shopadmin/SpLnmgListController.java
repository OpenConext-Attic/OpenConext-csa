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

package csa.control.shopadmin;

import csa.command.LmngServiceBinding;
import csa.domain.ServiceProvider;
import csa.model.LicenseStatus;
import csa.service.CrmService;
import csa.control.BaseController;
import csa.dao.CompoundServiceProviderDao;
import csa.dao.LmngIdentifierDao;
import csa.domain.CompoundServiceProvider;
import csa.service.ExportService;
import csa.service.ServiceProviderService;
import csa.service.impl.CompoundSPService;
import csa.service.impl.LmngUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping(value = "/shopadmin/*")
public class SpLnmgListController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(SpLnmgListController.class);

  @Resource
  private ServiceProviderService providerService;

  @Resource
  private CrmService licensingService;

  @Autowired
  private LmngIdentifierDao lmngIdentifierDao;

  @Resource
  private CompoundSPService compoundSPService;

  @Autowired
  private CompoundServiceProviderDao compoundServiceProviderDao;
  
  @Autowired
  private ExportService exportService;

  private LmngUtil lmngUtil = new LmngUtil();

  @RequestMapping(value = "/all-spslmng")
  public ModelAndView listAllSpsLmng(Map<String, Object> model) {
    List<LmngServiceBinding> lmngServiceBindings = getAllBindings();
    model.put("bindings", lmngServiceBindings);
    List<LmngServiceBinding> cspOrphans = getOrphans(lmngServiceBindings);
    model.put("orphans", cspOrphans);
    model.put("licenseStatuses", LicenseStatus.values());
    return new ModelAndView("shopadmin/sp-overview", model);
  }

  private List<LmngServiceBinding> getOrphans(List<LmngServiceBinding> lmngServiceBindings) {
    Set<String> spEntitySet = lmngServiceBindings.stream().
      filter( lmngServiceBinding -> lmngServiceBinding.getCompoundServiceProvider() != null).
      map( lmngServiceBinding -> lmngServiceBinding.getCompoundServiceProvider().getServiceProviderEntityId()).
      collect(Collectors.toSet());

    Iterable<CompoundServiceProvider> csps = compoundServiceProviderDao.findAll();
    Iterator<CompoundServiceProvider> cspIter = csps.iterator();
    while (cspIter.hasNext()) {
      CompoundServiceProvider current = cspIter.next();
      if (spEntitySet.contains(current.getServiceProviderEntityId())) {
        cspIter.remove();
      }
    }
    
    return StreamSupport.stream(csps.spliterator(), false).
      map(csp -> new LmngServiceBinding(csp.getLmngId(), csp.getServiceProvider(), csp)).
      collect(Collectors.toList());
  }

  private List<LmngServiceBinding> getAllBindings() {
    List<LmngServiceBinding> lmngServiceBindings = new ArrayList<>();
    for (ServiceProvider serviceProvider : providerService.getAllServiceProviders(false)) {
      String lmngIdentifier = lmngIdentifierDao.getLmngIdForServiceProviderId(serviceProvider.getId());
      CompoundServiceProvider compoundServiceProvider = compoundSPService.getCSPByServiceProvider(serviceProvider);
      lmngServiceBindings.add(new LmngServiceBinding(lmngIdentifier, serviceProvider, compoundServiceProvider));
    }
    return lmngServiceBindings;
  }
  
  @RequestMapping(value = "/export/csv")
  public @ResponseBody String exportToCSV(HttpServletRequest request, HttpServletResponse response, @RequestParam(value="type", required=false) String type) throws URISyntaxException {
    String result;
    List<LmngServiceBinding> lmngServiceBindings = getAllBindings();
    String baseUrl = getBaseUrl(request);
    
    if (StringUtils.isEmpty(type)) {
      result = exportService.exportServiceBindingsCsv(lmngServiceBindings, baseUrl);
    } else if (type.equalsIgnoreCase("orphans")) {
      List<LmngServiceBinding> cspOrphans = getOrphans(lmngServiceBindings);
      result = exportService.exportServiceBindingsCsv(cspOrphans, baseUrl);
    } else {
      throw new IllegalArgumentException("Unknown type given: " + type);
    }
    
    // set content headers for CSV
    response.setHeader("Content-Disposition", "attachment; filename=\"spEntityExport.csv\"");
    response.setContentType("text/csv");
    response.setContentLength(result.length());
    return result;
  }
  
  private String getBaseUrl(HttpServletRequest request) throws URISyntaxException {
    String result = "";
    
    URI myUri = new URI(request.getRequestURL().toString());
    result += myUri.getScheme()+"://"+myUri.getHost();
    if (myUri.getPort() > 0) {
      result += ":"+myUri.getPort();
    }
    
    return result;
  }

  @RequestMapping(value = "/save-splmng", method = RequestMethod.POST)
  public ModelAndView saveLmngServices(HttpServletRequest req) {
    Map<String, Object> model = new HashMap<>();

    String spId = req.getParameter("spIdentifier");
    String lmngId = req.getParameter("lmngIdentifier");
    Integer index = Integer.valueOf(req.getParameter("index"));

    String isClearPressed = req.getParameter("clearbutton");
    if (StringUtils.isBlank(lmngId) || StringUtils.isNotBlank(isClearPressed)) {
      log.debug("Clearing lmng identifier for ServiceProvider with ID " + spId);
      lmngId = null;
    } else {
      // extra validation (also done in frontend/jquery)
      if (!lmngUtil.isValidGuid(lmngId)) {
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

  @RequestMapping(value = "/update-enduser-visible/{cspId}/{newValue}", method = RequestMethod.PUT)
  public
  @ResponseBody
  String updateCspPublicApi(@PathVariable("cspId") Long cspId, @PathVariable("newValue") boolean newValue) {
    CompoundServiceProvider csp = compoundServiceProviderDao.findOne(cspId);
    csp.setAvailableForEndUser(newValue);
    compoundServiceProviderDao.save(csp);
    log.info("Updated CompoundServiceProvider(" + cspId + ") to be available for end users:" + newValue);
    return "ok";
  }

  @RequestMapping(value = "/update-license-status/{cspId}/{newValue}", method = RequestMethod.PUT)
  public
  @ResponseBody
  String updateCspLicenseStatus(@PathVariable("cspId") Long cspId, @PathVariable("newValue") String newValue) {
    CompoundServiceProvider csp = compoundServiceProviderDao.findOne(cspId);
    csp.setLicenseStatus(LicenseStatus.valueOf(newValue));
    compoundServiceProviderDao.save(csp);
    log.info("Updated CompoundServiceProvider(" + cspId + ") license status:" + newValue);
    return "ok";
  }

  @RequestMapping(value = "/delete-csp.shtml", method = RequestMethod.POST)
  public void deleteCompoundServiceProvider(@RequestParam("cspId") String postedCspId, HttpServletResponse response) throws IOException {
    log.info("deleting compound service provider with ID " + postedCspId);
    Long cspId = Long.parseLong(postedCspId);
    CompoundServiceProvider csp = compoundServiceProviderDao.findOne(cspId);
    compoundServiceProviderDao.delete(csp);
    
    //redirect to services page
    response.sendRedirect("all-spslmng.shtml");
  }
}
