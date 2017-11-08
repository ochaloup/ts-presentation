#!/bin/sh

function unzipTo {
  local TEMP=`mktemp`
  unzip -d "${TEMP}" "${1}"
  local DIR=`echo ${TEMP}/*`
  mv "${DIR}" "${2}"
}

WFLY_ZIP="wildfly-11.0.0.Final.zip"
WFLY_DOWNLOAD='http://download.jboss.org/wildfly/11.0.0.Final/${WFLY_ZIP}'

if [ 'x' = "x${JBOSS_HOME_1}" ] || [ 'x' = "x${JBOSS_HOME_1}" ]; then
    [ ! -f "$WFLY_ZIP" ] && wget "${WFLY_DOWNLOAD}"
    if [ 'x' = "x${JBOSS_HOME_1}" ]; then
        export JBOSS_HOME_1="${PWD}/jboss1"
        rm -rf "${JBOSS_HOME_1}"
        unzipTo "${WFLY_ZIP}" "${JBOSS_HOME_1}"
    fi
    if [ 'x' = "x${JBOSS_HOME_2}" ]; then
        export JBOSS_HOME_2="${PWD}/jboss2"
        rm -rf "${JBOSS_HOME_2}"
        unzipTo "${WFLY_ZIP}" "${JBOSS_HOME_2}"
    fi
fi 


[ -d "${JBOSS_HOME_1}" ] && [ -f "${JBOSS_HOME_1}/jboss-modules.jar" ] && echo "JBOSS_HOME_1 at '${JBOSS_HOME_1}' is not a jboss directory" && exit 1
[ -d "${JBOSS_HOME_2}" ] && [ -f "${JBOSS_HOME_2}/jboss-modules.jar" ] && echo "JBOSS_HOME_2 at '${JBOSS_HOME_2}' is not a jboss directory" && exit 1

"${JBOSS_HOME_1}/bin/add-user.sh" -a -u admin123 -p Password1! --silent
"${JBOSS_HOME_2}/bin/add-user.sh" -a -u admin123 -p Password1! --silent


docker run -p 5432:5432 -d 786c677bec44
mvn clean install; cp target/wfly-client.war ../wildfly-11.0.0.Final-client/standalone/deployments/
mvn clean install; cp target/wfly-server.jar ../wildfly-11.0.0.Final-server/standalone/deployments/
