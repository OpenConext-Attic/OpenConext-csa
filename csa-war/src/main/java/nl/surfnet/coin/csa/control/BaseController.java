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

package nl.surfnet.coin.csa.control;

import nl.surfnet.coin.csa.domain.IdentityProvider;

import nl.surfnet.coin.csa.service.IdentityProviderService;
import nl.surfnet.coin.csa.util.AjaxResponseException;
import nl.surfnet.coin.csa.util.SpringSecurity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

/**
 * Abstract controller used to set model attributes to the request
 */
@Controller
public abstract class BaseController {

  /**
   * The name of the key under which all compoundSps (e.g. the services) are
   * stored
   */
  public static final String COMPOUND_SPS = "compoundSps";

  /**
   * The name of the key under which all identityproviders are stored
   */
  public static final String ALL_IDPS = "allIdps";

  /**
   * The name of the key under which a compoundSps (e.g. the service) is stored
   * for the detail view
   */
  public static final String COMPOUND_SP = "compoundSp";

  /**
   * The name of the key under which we store the info if a logged user is
   * allowed to request connections / disconnects
   */
  public static final String SERVICE_APPLY_ALLOWED = "applyAllowed";

  /**
   * The name of the key under which we store the info if a logged user is
   * allowed to ask questions
   */
  public static final String SERVICE_QUESTION_ALLOWED = "questionAllowed";

  /**
   * The name of the key under which we store the info if the status of a
   * technical connection is visible to the current user.
   */
  public static final String SERVICE_CONNECTION_VISIBLE = "connectionVisible";

  /**
   * The name of the key under which we store the info if the connection facet is visible to the current user.
   */
  public static final String FACET_CONNECTION_VISIBLE = "facetConnectionVisible";

  /**
   * The name of the key under which we store the info if a logged user is
   * allowed to filter in the app grid
   */
  public static final String FILTER_APP_GRID_ALLOWED = "filterAppGridAllowed";

  /**
   * The name of the key under which we store the info if a logged user is a
   * kind of admin
   */
  public static final String IS_ADMIN_USER = "isAdminUser";

  /**
   * The name of the key that defines whether a deeplink to SURFMarket should be
   * shown.
   */
  public static final String DEEPLINK_TO_SURFMARKET_ALLOWED = "deepLinkToSurfMarketAllowed";

  /**
   * The name of the key under which we store the info if the logged in user is
   * Distribution Channel Admin (aka God)
   */
  public static final String IS_GOD = "isGod";

  /**
   * The name of the key under which we store the token used to prevent session
   * hijacking
   */
  public static final String TOKEN_CHECK = "tokencheck";

  /**
   * The name of the key under which we store the information from Api regarding
   * group memberships and actual members for auto-completion in the
   * recommendation modal popup.
   */
  public static final String GROUPS_WITH_MEMBERS = "groupsWithMembers";

  /**
   * Key in which we store whether a user should see the technical attribute names of an ARP.
   */
  public static final String RAW_ARP_ATTRIBUTES_VISIBLE = "rawArpAttributesVisible";

  /**
   * Key in which we store the currently selected IdP
   */
  protected static final String SELECTED_IDP = "selectedIdp";

  @Resource(name = "providerService")
  private IdentityProviderService idpService;

  @Resource(name = "localeResolver")
  protected LocaleResolver localeResolver;

  @ModelAttribute(value = "idps")
  public List<IdentityProvider> getMyInstitutionIdps() {
    return SpringSecurity.getCurrentUser().getInstitutionIdps();
  }

  @ModelAttribute(value = "locale")
  public Locale getLocale(HttpServletRequest request) {
    return localeResolver.resolveLocale(request);
  }

  protected IdentityProvider getSelectedIdp(HttpServletRequest request) {
    final IdentityProvider selectedIdp = (IdentityProvider)  request.getSession().getAttribute(SELECTED_IDP);
    if (selectedIdp != null) {
      return selectedIdp;
    }
    return selectProvider(request, SpringSecurity.getCurrentUser().getIdp().getId());
  }

  protected IdentityProvider switchIdp(HttpServletRequest request, String switchIdpId) {
    Assert.hasText(switchIdpId);
    return selectProvider(request, switchIdpId);
  }

  private IdentityProvider selectProvider(HttpServletRequest request, String idpId) {
    Assert.hasText(idpId);
    for (IdentityProvider idp : SpringSecurity.getCurrentUser().getInstitutionIdps()) {
      if (idp.getId().equals(idpId)) {
        request.getSession().setAttribute(SELECTED_IDP, idp);
        SpringSecurity.getCurrentUser().setIdp(idp);
        return idp;
      }
    }
    throw new RuntimeException(idpId + " is unknown for " + SpringSecurity.getCurrentUser().getUsername());
  }


  /**
   * Handler for {@link AjaxResponseException}. We don't want a 500, but a 400
   * and we want to stream the error message direct to the javaScript
   *
   * @param e the exception
   * @return the response body
   */
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  @ResponseBody
  @ExceptionHandler(AjaxResponseException.class)
  public Object handleAjaxResponseException(AjaxResponseException e) {
    return e.getMessage();
  }

}
