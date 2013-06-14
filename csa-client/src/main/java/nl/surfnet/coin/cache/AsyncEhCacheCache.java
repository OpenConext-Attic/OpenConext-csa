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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.cache.Cache;
import org.springframework.cache.ehcache.EhCacheCache;

public class AsyncEhCacheCache extends EhCacheCache {

  /**
   * Create an {@link org.springframework.cache.ehcache.EhCacheCache} instance.
   *
   * @param ehcache backing Ehcache instance
   */
  public AsyncEhCacheCache(Ehcache ehcache) {
    super(ehcache);
  }

  @Override
  public ValueWrapper get(Object key) {
    Element element = getNativeCache().getQuiet(key);
    boolean expired = element.isExpired();
    if (expired) {
      //TODO we need a hook to invoke the underlying method - not possible
    //  Put CachePut on the cache methods
      //this wont work. we will schedule a tread every hour to refresh cache and put the time - to - live higher then that???
    }

    return super.get(key);
  }
}
