package csa.util;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.*;

public class LicenseContactPersonServiceTest {

  private LicenseContactPersonService subject = new LicenseContactPersonService(new ClassPathResource("license_contact_persons_surfmarket.csv"));

  @Test
  public void test_parsing() throws Exception {
    subject.onApplicationEvent(null);
  }

}
