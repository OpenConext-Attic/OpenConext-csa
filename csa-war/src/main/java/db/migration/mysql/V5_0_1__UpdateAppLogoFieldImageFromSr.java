/*
 *
 *  * Copyright 2013 SURFnet bv, The Netherlands
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package db.migration.mysql;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.Field;
import nl.surfnet.coin.janus.Janus;
import nl.surfnet.coin.janus.domain.EntityMetadata;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.googlecode.flyway.core.migration.java.JavaMigration;

/**
 * BACKLOG-1126
 * <p/>
 * In order to use the rectangle SP-logo's as configured in Service Registry instead of the custom ones from CDK we will
 * update all FieldImages if there is a correct logo image in SR (otherwise we don't update anything).
 */
public class V5_0_1__UpdateAppLogoFieldImageFromSr implements JavaMigration {

  private static final Logger LOG = LoggerFactory.getLogger(V5_0_1__UpdateAppLogoFieldImageFromSr.class);


  @Override
  public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
    Properties props = new Properties();
    props.load(new ClassPathResource("csa.properties").getInputStream());
    
    Janus janus = getJanusClass(props.getProperty("janus.class"));

    janus.setJanusUri(new URI(props.getProperty("janus.uri")));
    janus.setSecret(props.getProperty("janus.secret"));
    janus.setUser(props.getProperty("janus.user"));

    String query = "select id, service_provider_entity_id from compound_service_provider";
    List<Object[]> serviceProviderArrs = jdbcTemplate.query(query, new RowMapper<Object[]>() {
      @Override
      public Object[] mapRow(ResultSet resultSet, int i) throws SQLException {
        return new Object[]{resultSet.getLong("id"), resultSet.getString("service_provider_entity_id")};
      }
    });
    for (Object[] serviceProviderArr : serviceProviderArrs) {
      String entityId = (String) serviceProviderArr[1];
      EntityMetadata metadata;
      try {
        metadata = janus.getMetadataByEntityId(entityId);
      } catch (Throwable e) {
        LOG.warn("SR reported an exception for retrieving the metadata for {}", entityId );
        continue;
      }
      if (metadata != null) {
        String appLogoUrl = metadata.getAppLogoUrl();
        if (StringUtils.isNotBlank(appLogoUrl) && !appLogoUrl.equalsIgnoreCase(CompoundServiceProvider.SR_DEFAULT_LOGO_VALUE)) {
          int update = jdbcTemplate.update("UPDATE field_image SET field_source = " + Field.Source.SURFCONEXT.ordinal() +
                  " WHERE compound_service_provider_id = " + serviceProviderArr[0] +
                  " AND field_key = " + Field.Key.APPSTORE_LOGO.ordinal());
          LOG.info("Updated {} record in field_image table to use the SURFCONEXT source for the App Store Logo {} for {}", update, appLogoUrl, entityId);
        }
      }
    }
  }
  
  private Janus getJanusClass(String clazz) {
    try {
      return (Janus) Class.forName(clazz).newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }
}
