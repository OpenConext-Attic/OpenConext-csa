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
package nl.surfnet.coin.csa.util;

import nl.surfnet.coin.csa.domain.Field;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertTrue;

public class ResourceBundleUtils {

  private static final String WEB_INF_DIR = "csa-war/src/main/webapp/WEB-INF";

  private Pattern springMessagePattern = Pattern.compile("<spring:message.*?code=\"([\\w_\\-\\.]+)\"");

  private static final List<String> KNOWN_MISSING = new ArrayList<String>();

  static {
    Field.Key[] keys = Field.Key.values();
    for (Field.Key key : keys) {
      KNOWN_MISSING.add("jsp.compoundSp." + key.name());
    }

    KNOWN_MISSING.addAll(Arrays.asList(new String[]{
            "jsp.role.information.key.ROLE_DISTRIBUTION_CHANNEL_ADMIN",
            "datatables.sSearch",
            "datatables.sZeroRecords",
            "datatables.sInfo",
            "datatables.sInfoEmpty",
            "datatables.sInfoFiltered",
            "LMNG",
            "SURFCONEXT",
            "DISTRIBUTIONCHANNEL",
    }));

  }

  @Test
  public void findUsedProperties() throws IOException {
    //IOUtils.writeLines(findUnusedKeys(), null, System.out);
  }

  private List<String> findUnusedKeys() throws IOException {
    Properties resourceBundle = new Properties();

    resourceBundle.load(new ClassPathResource("messages_nl.properties").getInputStream());
    Set<Object> keys = resourceBundle.keySet();

    File webInfFolder = new File(WEB_INF_DIR);
    assertTrue(webInfFolder.getAbsolutePath(), webInfFolder.exists());

    Collection<File> files = FileUtils.listFiles(webInfFolder, new String[]{"jsp", "xml"}, true);
    Set<String> codes = new HashSet<String>();
    List<String> result = new ArrayList<String>();
    for (File file : files) {
      Matcher m = springMessagePattern.matcher(FileUtils.readFileToString(file));
      while (m.find()) {
        codes.add(m.group(1));
      }
    }
    for (Object key : keys) {
      if (codes.contains(key) || KNOWN_MISSING.contains(key)) {
        result.add(key + "=" + resourceBundle.getProperty((String) key));
      }
    }
    Collections.sort(result);
    return result;
  }
}

