package nl.surfnet.coin.csa.service.impl;/*
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

import nl.surfnet.coin.csa.domain.Account;
import nl.surfnet.coin.csa.domain.Article;
import nl.surfnet.coin.csa.model.License;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface CrmUtil {

  public enum LicenseRetrievalAttempt{ One, Two, Three}

  List<Article> parseArticlesResult(String webserviceResult, boolean writeResponseToFile)
          throws ParserConfigurationException, SAXException, IOException, ParseException ;

  List<License> parseLicensesResult(String webserviceResult, boolean writeResponseToFile)
          throws ParserConfigurationException, SAXException, IOException, ParseException ;

  List<Account> parseAccountsResult(String webserviceResult, boolean writeResponseToFile)
          throws ParserConfigurationException, SAXException, IOException, ParseException ;

  String parseResultInstitute(String webserviceResult, boolean writeResponseToFile) throws ParserConfigurationException,
          SAXException, IOException, ParseException ;

  public String getLmngSoapRequestForIdpAndSp(String institutionId, List<String> serviceIds, Date validOn, String endpoint, LicenseRetrievalAttempt licenseRetrievalAttempt) throws IOException;

  void writeIO(String filename, String content);

  String getLmngSoapRequestForSps(Collection<String> serviceIds, String endpoint) throws IOException ;

  String getLmngSoapRequestForAllAccount(boolean isInstitution, String endpoint) throws IOException ;

  String getLmngRequestEnvelope() throws IOException ;

}
