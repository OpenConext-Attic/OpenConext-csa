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
package nl.surfnet.coin.cache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import org.springframework.cache.Cache;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.LinkedHashSet;

public class AsyncEhCacheCacheManager extends EhCacheCacheManager {

  @Override
  protected Collection<Cache> loadCaches() {
    CacheManager cacheManager = getCacheManager();
    Assert.notNull(cacheManager, "A backing EhCache CacheManager is required");
    Status status = cacheManager.getStatus();
    Assert.isTrue(Status.STATUS_ALIVE.equals(status),
            "An 'alive' EhCache CacheManager is required - current cache is " + status.toString());

    String[] names = cacheManager.getCacheNames();
    Collection<Cache> caches = new LinkedHashSet<Cache>(names.length);
    for (String name : names) {
      caches.add(new AsyncEhCacheCache(cacheManager.getEhcache(name)));
    }
    return caches;

  }
}
