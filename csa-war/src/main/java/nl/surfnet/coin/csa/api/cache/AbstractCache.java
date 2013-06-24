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
package nl.surfnet.coin.csa.api.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

public abstract class AbstractCache implements InitializingBean {

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractCache.class);

  private @Value("${cacheMillisecondsStartupDelayTime}") long delay;

  private @Value("${cacheMillisecondsProviders}") long duration;

  @Override
  public void afterPropertiesSet() throws Exception {
    this.scheduleRefresh();
  }

  private void scheduleRefresh() {
    Timer timer = new Timer();

    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        LOG.info("Starting refreshing {} cache", getCacheName());
        try {
            doAsyncScheduleAtFixedRate();
        } catch (Throwable t) {
          /*
           * Looks like anti pattern, but otherwise the repeated timer stops. See:
           *
           * http://stackoverflow.com/questions/8743027/java-timer-class-timer-tasks-stop-to-execute-if-in-one-of-the-tasks-exception-i
           *
           */
          LOG.error("Error in the refresh of the cache", t);
        } finally {
          LOG.info("Finished refreshing {} cache", getCacheName());
        }
      }
    }, getDelay(), getDuration());
  }

  protected abstract void doAsyncScheduleAtFixedRate() throws Exception;
  protected abstract String getCacheName();

  public long getDelay() {
    return delay;
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }
}
