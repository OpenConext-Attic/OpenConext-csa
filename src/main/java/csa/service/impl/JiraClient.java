/*
 * Copyright 2012 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csa.service.impl;

import java.io.IOException;
import java.util.List;

import csa.domain.CoinUser;
import csa.model.JiraTask;

public interface JiraClient {

  /**
   * Create a new task in Jira.
   *
   * @param task the task you want to create
   * @param user the user which issued the request
   * @return the new task key
   * @throws IOException when communicating with jira fails
   */
  String create(final JiraTask task, CoinUser user) throws IOException;

  /**
   * Retrieve specific tasks from Jira.
   *
   * @param keys a list of the task keys you want to retrieve
   * @return a list of tasks
   * @throws IOException when communicating with jira fails
   */
  List<JiraTask> getTasks(final List<String> keys) throws IOException;

}
