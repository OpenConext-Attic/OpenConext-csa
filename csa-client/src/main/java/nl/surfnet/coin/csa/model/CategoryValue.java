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

package nl.surfnet.coin.csa.model;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

public class CategoryValue implements Comparable<CategoryValue>{

  private int count;
  private String value;

  public CategoryValue(String value) {
    this.value = value;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getCount() {
    return count;
  }

  public String getValue() {
    return value;
  }
  
/*
 * The value of a FacetValue may contain spaces, but if we want to search in (any) clients, then we
 * want to be able to have all the FacetValues of a Service separated by spaces therefore this method
 * can be used to underscore-separate the different FacetValues
 */
  @JsonIgnore
  public String getSearchValue() {
    String val = getValue();
    return val != null ? val.replaceAll(" ", "_").toLowerCase() : val;
  }

  @Override
  public int compareTo(CategoryValue o) {
    return new CompareToBuilder()
      .append(this.value, o.value)
      .toComparison();
  }
}
