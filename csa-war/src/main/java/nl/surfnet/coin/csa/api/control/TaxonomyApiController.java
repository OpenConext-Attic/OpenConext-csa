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

import nl.surfnet.coin.csa.model.*;
import nl.surfnet.coin.csa.dao.FacetDao;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.sort;

@Controller
@RequestMapping
public class TaxonomyApiController extends BaseApiController{

  @Resource
  private FacetDao facetDao;

  @RequestMapping(method = RequestMethod.GET, value = "/api/public/taxonomy.json")
  @Cacheable(value = "csaApi")
  public @ResponseBody
  Taxonomy getTaxonomy(@RequestParam(value = "lang", defaultValue = "en") String language) {
    List<Facet> facets = facetDao.findAll();
    List<Category> categories = new ArrayList<Category>();
    for (Facet facet : facets) {
      Category category = new Category(facet.getName());
      categories.add(category);
      List<CategoryValue> values = new ArrayList<CategoryValue>();
      for (FacetValue facetValue : facet.getFacetValues()) {
        values.add(new CategoryValue(facetValue.getValue())) ;
      }
      category.setValues(values);
    }
    return new Taxonomy(categories);
  }
}
