package nl.surfnet.coin.csa.service.impl;

import static junit.framework.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.util.ConcurrentRunner;
import nl.surfnet.coin.csa.util.ConcurrentRunnerContext;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
  "classpath:applicationContext.xml",
  "classpath:coin-csa-with-real-cache.xml"
})
@TransactionConfiguration(transactionManager = "csaTransactionManager", defaultRollback = true)
@Transactional
public class CSPInitializationTest {

  @Autowired
  private CompoundSPService cspSvc;

  @Test
  public void test() {
    List<Integer> results = new ConcurrentRunnerContext<Integer>(10).run(new ConcurrentRunner() {
      @Override
      public Integer run() {
        List<CompoundServiceProvider> csps = cspSvc.getCSPsByIdp(new IdentityProvider("http://mock-idp", "institutionId", "name"));
        return csps.size();
      }
    });
    for (Integer oneResult : results) {
      assertEquals("all results of getCSPsByIdp should be the same", 4, oneResult.intValue());
    }
  }
}
