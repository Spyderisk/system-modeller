#!/bin/sh

sed -e s/KEYCLOAK_CREDENTIALS_SECRET/${KEYCLOAK_CREDENTIALS_SECRET}/ /opt/keycloak/data/import/ssm.template > /opt/keycloak/data/import/ssm.json
/opt/keycloak/bin/kc.sh start-dev --import-realm --http-relative-path /auth
