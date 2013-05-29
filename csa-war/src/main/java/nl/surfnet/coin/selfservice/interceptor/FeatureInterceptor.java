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

package nl.surfnet.coin.selfservice.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.surfnet.coin.selfservice.util.SpringSecurity;

import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Interceptor to enable/disable (new) features
 */
public class FeatureInterceptor extends HandlerInterceptorAdapter {
  private boolean dev;
  private boolean showOauthTokens;
  private boolean ebLinkActive;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    request.setAttribute("dev", dev); // for use on error page
    request.setAttribute("showOauthTokens", showOauthTokens);
    request.setAttribute("ebLinkActive", ebLinkActive);
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    super.postHandle(request, response, handler, modelAndView);

    if (modelAndView != null) {
      final ModelMap map = modelAndView.getModelMap();
      map.addAttribute("dev", dev);
      map.addAttribute("showOauthTokens", showOauthTokens);
      map.addAttribute("ebLinkActive", ebLinkActive);
      map.addAttribute("roles", SpringSecurity.getCurrentUser().getAuthorities());
    }
  }

  public void setDev(boolean dev) {
    this.dev = dev;
  }

  /**
   * @return the showOauthTokens
   */
  public boolean isShowOauthTokens() {
    return showOauthTokens;
  }

  /**
   * @param showOauthTokens the showOauthTokens to set
   */
  public void setShowOauthTokens(boolean showOauthTokens) {
    this.showOauthTokens = showOauthTokens;
  }

  /**
   * @return the ebLinkActive
   */
  public boolean isEbLinkActive() {
    return ebLinkActive;
  }

  /**
   * @param ebLinkActive the ebLinkActive to set
   */
  public void setEbLinkActive(boolean ebLinkActive) {
    this.ebLinkActive = ebLinkActive;
  }
  
}
