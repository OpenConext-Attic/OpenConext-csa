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

import nl.surfnet.coin.csa.model.*;
import nl.surfnet.coin.selfservice.dao.FacetDao;
import nl.surfnet.coin.selfservice.domain.Article;
import nl.surfnet.coin.selfservice.domain.CompoundServiceProvider;
import nl.surfnet.coin.selfservice.domain.IdentityProvider;
import nl.surfnet.coin.selfservice.domain.Provider.Language;
import nl.surfnet.coin.selfservice.service.IdentityProviderService;
import nl.surfnet.coin.selfservice.service.LmngService;
import nl.surfnet.coin.selfservice.service.impl.CompoundSPService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.surfnet.oaaas.auth.AuthorizationServerFilter;
import org.surfnet.oaaas.auth.principal.AuthenticatedPrincipal;
import org.surfnet.oaaas.conext.SAMLAuthenticatedPrincipal;
import org.surfnet.oaaas.model.VerifyTokenResponse;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.sort;

@Controller
@RequestMapping
public class TaxonomyApiController {

  @Resource
  private FacetDao facetDao;

  @RequestMapping(method = RequestMethod.GET, value = "/api/public/taxonomy.json")
  public @ResponseBody
  Taxonomy getTaxonomy(@RequestParam(value = "lang", defaultValue = "en") String language) {
    List<Facet> facets = facetDao.findAll();
    List<Category> categories = new ArrayList<Category>();
    for (Facet facet : facets) {
      categories.add(new Category(facet.getName()));
      List<String> values = new ArrayList<String>();
      for (FacetValue facetValue : facet.getFacetValues()) {
        values.add(facetValue.getValue()) ;
      }
    }
    return new Taxonomy(categories);
  }
}
