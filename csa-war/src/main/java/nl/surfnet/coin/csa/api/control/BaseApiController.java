/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.surfnet.coin.csa.api.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.surfnet.oaaas.auth.AuthorizationServerFilter;
import org.surfnet.oaaas.auth.principal.AuthenticatedPrincipal;
import org.surfnet.oaaas.conext.SAMLAuthenticatedPrincipal;
import org.surfnet.oaaas.model.VerifyTokenResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public abstract class BaseApiController {

  private static final Logger LOG = LoggerFactory.getLogger(BaseApiController.class);

  /*
 * Retrieve IDP Entity ID from the oauth token stored in the request
 *
 */
  protected String getIdpEntityIdFromToken(final HttpServletRequest request) {
    VerifyTokenResponse verifyTokenResponse = (VerifyTokenResponse) request.getAttribute(AuthorizationServerFilter.VERIFY_TOKEN_RESPONSE);
    AuthenticatedPrincipal authenticatedPrincipal = verifyTokenResponse.getPrincipal();
    if (authenticatedPrincipal instanceof SAMLAuthenticatedPrincipal) {
      SAMLAuthenticatedPrincipal principal = (SAMLAuthenticatedPrincipal) authenticatedPrincipal;
      return principal.getIdentityProvider();
    }
    throw new IllegalArgumentException("Only type of Principal supported is SAMLAuthenticatedPrincipal, not " + authenticatedPrincipal.getClass());
  }

  protected void verifyScope(HttpServletRequest request, String scopeRequired) {
    VerifyTokenResponse verifyTokenResponse = (VerifyTokenResponse) request.getAttribute(AuthorizationServerFilter.VERIFY_TOKEN_RESPONSE);
    List<String> scopes = verifyTokenResponse.getScopes();
    if (CollectionUtils.isEmpty(scopes) || !scopes.contains(scopeRequired)) {
      throw new ScopeVerificationException("Scope required is '" + scopeRequired + "', but not granted. Granted scopes: " + scopes);
    }
  }

  @ExceptionHandler(ScopeVerificationException.class)
  public void handleSecurityException(Exception ex, HttpServletResponse response) throws IOException {
    LOG.info("Will return 403 Forbidden", ex);
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Granted scope not sufficient");
    response.flushBuffer();
  }

  @ExceptionHandler(Exception.class)
  public void handleGenericException(Exception ex, HttpServletResponse response) throws IOException {
    LOG.info("Will return 409 Not found", ex);
    response.sendError(HttpServletResponse.SC_CONFLICT, "The request could not be completed due to a conflict with the current state of the resource.");
    response.flushBuffer();
  }

}