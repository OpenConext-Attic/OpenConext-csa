<!--
  Copyright 2012 SURFnet bv, The Netherlands

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# RELEASE INFORMATION

    Project:           CSA

    Content:
      1.  Prepare Tomcat
        1.1 Stop Tomcat
        1.2 Undeploy a previous version
        1.3 Copy / edit property files
      2.  Deploy war file
      3.  Start tomcat


## 1. PREPARE TOMCAT

This installation document only provides documentation for the Tomcat application server.

If you already have deployed a previous version of the csa application
you must follow step 1.2a to undeploy the previous version.

If you have not deployed a previous version yet, follow 1.2b instead.

### 1.1 Stop Tomcat

Stop the tomcat application server

### 1.2a Undeploy a previous version (optional)
Navigate to the `<<CATALINA_HOME>>/wars/`
(e.g. /opt/tomcat/wars/)
delete the `csa-war-<<VERION>>.war` file.
(e.g. csa-war-2.6.0-SNAPSHOT.war)

Navigate to `<<CATALINA_HOME>>/work/csa.{dev,test,acc}.surfconext.nl/`
Delete the entire csa directory listed there.

### 1.2b Prepare Tomcat for first time deployment
Create a webapps holder directory:
`<<CATALINA_HOME>>/webapps/csa.{dev,test,acc}.surfconext.nl/`
and give ownership to tomcat:
`chown tomcat:tomcat <<CATALINA_HOME>>/webapps/csa.{dev,test,acc}.surfconext.nl/`


### 1.3 Copy / edit property files

Out-of-the-box the tarball comes with a number of different property files.
A number of property files are delivered:

- csa.properties.<<ENV>>
- csa-ehcache.xml.<<ENV>>


For different environments different property files are delivered. Pick the
appropriate property file for your environment from the following directory:
`<<EXTRACTED_TAR_BALL_PATH>>/tomcat/conf/classpath_properties`

Copy the chosen property files to:
`<<CATALINA_HOME>>/conf/classpath_properties/csa.properties`
`<<CATALINA_HOME>>/conf/classpath_properties/csa-ehcache.xml`
`<<CATALINA_HOME>>/conf/classpath_properties/csa-logback.xml`

Edit the values of the property files according to your environment.


## 2. DEPLOY WAR FILE

Copy the provided context descriptor from
`<<EXTRACTED_TAR_BALL_PATH>>/tomcat/conf/context`
to
`<<CATALINA_HOME>>/conf/Catalina/<<CSA-VIRTUAL-HOST-DIRECTORY>>`
(e.g. /opt/tomcat/conf/Catalina/csa.dev.surfconext.nl)

Now, copy the coin-portal war located at
`<<EXTRACTED_TAR_BALL_PATH>>/tomcat/webapps`
to
`<<CATALINA_HOME>>/wars/`
(e.g. /opt/tomcat/wars/


## 3. START TOMCAT

Start tomcat again.
