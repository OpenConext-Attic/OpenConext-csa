package nl.surfnet.coin.csa.model;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.Calendar;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class LicenseTest {

  @Test
  public void testIsValid() throws Exception {
    License license = new License();
    assertTrue(license.isValid());

    license.setEndDate(new LocalDate(2000,1,1).toDate());
    assertFalse(license.isValid());

    license.setEndDate(new LocalDate(2999,1,1).toDate());
    assertTrue(license.isValid());

  }
}
