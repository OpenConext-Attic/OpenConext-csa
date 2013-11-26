package db.migration.mysql;

import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.googlecode.flyway.core.migration.java.JavaMigration;

public class V4_0_2__ConfigurableNameInCSP implements JavaMigration {

  @Override
  public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
    Properties props = new Properties();
    props.load(new ClassPathResource("csa.properties").getInputStream());

    if (!Boolean.valueOf(props.getProperty("csa.migration.skip"))) {
      String query = "select id from compound_service_provider"; 
      List<BigInteger> cspIds = jdbcTemplate.queryForList(query, BigInteger.class);
      for (BigInteger id : cspIds) {
        jdbcTemplate.update("insert into field_string (field_key, field_source, field_value, compound_service_provider_id)" +
        		"values (16, 1, NULL, ?)", id);
        jdbcTemplate.update("insert into field_string (field_key, field_source, field_value, compound_service_provider_id)" +
            "values (17, 1, NULL, ?)", id);
      }
    }
  }
}
