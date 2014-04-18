# SURFconext CSA

## About OpenConext

OpenConext is an OpenSource technology stack for creating and running Collaboration platforms. It uses technologies from Federated Identity Management, as is available in Research and Educational Access Federations, Group management and OpenSocial Social Networking Technology. The aim of the software is to provide a middleware platform that can combine generic and specialized collaboration tools and services, within Research and Education, and beyond, and make these available for collaboration over institutional and national borders. The features section describes the current and planned features for the platform.

OpenConext was developed by SURFnet as part of the SURFworks programme. SURFnet runs an instance of the platform for research and education in The Netherlands as SURFconext


OpenConext: [http://www.openconext.org](http://www.openconext.org)

SURFconext: [http://www.surfconext.nl](http://www.surfconext.nl)


## Getting started

### System Requirements

- Java 6
- Maven 3
- MySQL 5.5

### Building and running

[Maven 3](http://maven.apache.org) is needed to build and run this project.

To build:

    mvn clean install

To run locally:

    cd coin-csa-war
    mvn jetty:run


There is an [outstanding issue](https://cwiki.apache.org/confluence/display/MAVEN/AetherClassNotFound). If you don't want to downgrade maven (using brew) then you can manually compile the css:

    compass compile src/main/webapp src/main/webapp/sass/screen.sass && cp src/main/webapp/css/screen.css target/csa-war-4.0.2-SNAPSHOT/css/sreen.css