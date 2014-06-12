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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class SerializationTest {

  private ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

  @Test
  public void service() throws IOException, InvocationTargetException, IllegalAccessException {
    Service service = objectMapper.readValue(new ClassPathResource("json/service.json").getInputStream(), Service.class);
    service.restoreCategoryReferences();

    String s = objectMapper.writeValueAsString(service) ;
    service = objectMapper.readValue(s, Service.class);

    assertTrue(service.getLicense().isValid());

  }
}
