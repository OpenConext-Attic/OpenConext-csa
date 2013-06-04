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

package nl.surfnet.coin.csa.api.control;

import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.domain.ServiceProvider;
import nl.surfnet.coin.csa.interceptor.AuthorityScopeInterceptor;
import nl.surfnet.coin.csa.model.Action;
import nl.surfnet.coin.csa.model.InstitutionIdentityProvider;
import nl.surfnet.coin.csa.service.ActionsService;
import nl.surfnet.coin.csa.service.EmailService;
import nl.surfnet.coin.csa.service.IdentityProviderService;
import nl.surfnet.coin.csa.service.ServiceProviderService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping
public class ServiceRegistryController extends BaseApiController {


  private static final Logger LOG = LoggerFactory.getLogger(ServiceRegistryController.class);

  @Resource
  private IdentityProviderService identityProviderService;

  @RequestMapping(method = RequestMethod.GET, value = "/api/protected/identityproviders.json")
  public @ResponseBody
  List<InstitutionIdentityProvider> listActions(@RequestParam(value = "identityProviderId") String identityProviderId, HttpServletRequest request) throws IOException {
    List<InstitutionIdentityProvider> result = new ArrayList<InstitutionIdentityProvider>();
    IdentityProvider identityProvider = identityProviderService.getIdentityProvider(identityProviderId);
    if (identityProvider != null) {
      String institutionId = identityProvider.getInstitutionId();
      if (StringUtils.isBlank(institutionId)) {
        result.add(convertIdentityProviderToInstitutionIdentityProvider(identityProvider)) ;
      } else {
        List<IdentityProvider> instituteIdentityProviders = identityProviderService.getInstituteIdentityProviders(institutionId);
        for (IdentityProvider provider : instituteIdentityProviders) {
          result.add(convertIdentityProviderToInstitutionIdentityProvider(provider)) ;
        }
      }
    }
    return result;
  }

  private InstitutionIdentityProvider convertIdentityProviderToInstitutionIdentityProvider(IdentityProvider identityProvider) {
    return new InstitutionIdentityProvider(identityProvider.getId(), identityProvider.getName(), identityProvider.getInstitutionId());
  }

}
