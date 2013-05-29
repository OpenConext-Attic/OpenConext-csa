package nl.surfnet.coin.csa.service.impl;

import java.util.List;

import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.IdentityProvider;
import nl.surfnet.coin.csa.util.ConcurrentRunner;
import nl.surfnet.coin.csa.util.ConcurrentRunnerContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.Assert.assertEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
  "classpath:coin-csa-context.xml",
  "classpath:coin-csa-with-real-cache.xml",
  "classpath:coin-csa-properties-context.xml",
  "classpath:coin-shared-context.xml"
})
@TransactionConfiguration(transactionManager = "csaTransactionManager", defaultRollback = true)
@Transactional
public class CSPInitializationTest {

  @Autowired
  private CompoundSPService cspSvc;

  @Test
  public void test() {
    List<Integer> results = new ConcurrentRunnerContext<Integer>(20).run(new ConcurrentRunner() {
      @Override
      public Integer run() {
        List<CompoundServiceProvider> csps = cspSvc.getCSPsByIdp(new IdentityProvider("id", "institutionId", "name"));
        return csps.size();
      }
    });
    for (Integer oneResult : results) {
      assertEquals("all results of getCSPsByIdp should be the same", 58, oneResult.intValue());
    }}
}
