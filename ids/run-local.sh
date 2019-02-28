#!/usr/bin/env bash

#
# This script configures and runs the ID service using an embedded H2 database
# instead of the MySQL database used in normal situations. The H2 database will
# will be empty, but is suitable for testing.
#

cd $(dirname $0)

set -e
mvn clean package -DskipTests -P'!standard'

echo "Running ID service with embedded H2 database"

if [ -z "$HEALTH_APIS_CERT_PASSWORD" ]
then
  echo "HEALTH_APIS_CERT_PASSWORD is not set"
  echo "Attempting to discover it from most frequently used password in local configurations"
  HEALTH_APIS_CERT_PASSWORD=$(grep -E 'ssl.*password' config/*properties \
    | cut -d = -f 2 \
    | uniq -c \
    | sort -nr \
    | head -1 \
    | awk '{print $2}')
  [ -z $HEALTH_APIS_CERT_PASSWORD ] \
    && echo "Could not discover SSL keystore password from existing configurations" \
    && exit 1
fi

APPLICATION_JAR=$(find target -maxdepth 1 -type f -name "ids-*.jar")
APPLICATION_CLASS=gov.va.api.health.ids.service.Application

CLASSPATH=$APPLICATION_JAR:target/h2.jar 

#
# Deal with windows path which wants semi's. Also make sure there are no spaces by
# using DOS names.
#
shopt -s nocasematch
[[ "$OS" =~ windows.* ]] && CLASSPATH=$(cygpath --path --dos --mixed "$CLASSPATH") 


java \
  -cp "$CLASSPATH" \
  -Dloader=$APPLICATION_CLASS \
  -Dspring.jpa.generate-ddl=true \
  -Dspring.jpa.hibernate.ddl-auto=create-drop \
  -Dspring.datasource.driver-class-name=org.h2.Driver \
  -Dspring.datasource.url=jdbc:h2:mem:whatever  \
  -Dserver.ssl.key-store=file:target/certs/system/DVP-DVP-NONPROD.jks \
  -Dserver.ssl.key-store-password=$HEALTH_APIS_CERT_PASSWORD \
  -Dserver.ssl.key-alias=internal-sys-dev \
  -Dssl.key-store=file:target/certs/system/DVP-DVP-NONPROD.jks \
  -Dssl.key-store-password=$HEALTH_APIS_CERT_PASSWORD \
  -Dssl.client-key-password=$HEALTH_APIS_CERT_PASSWORD \
  -Dssl.use-trust-store=false \
  -Dssl.trust-store=file:target/certs/system/DVP-NONPROD-truststore.jks \
  -Dssl.trust-store-password=$HEALTH_APIS_CERT_PASSWORD \
  org.springframework.boot.loader.PropertiesLauncher
