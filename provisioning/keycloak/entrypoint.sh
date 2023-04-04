#!/bin/sh

whoami
ls -ld /opt/keycloak/data/import/
ls -l /opt/keycloak/data/import/
sed -e s/KEYCLOAK_CREDENTIALS_SECRET/${KEYCLOAK_CREDENTIALS_SECRET}/ /tmp/import/ssm.template > /opt/keycloak/data/import/ssm.json
/opt/keycloak/bin/kc.sh start-dev --import-realm --http-relative-path /auth
