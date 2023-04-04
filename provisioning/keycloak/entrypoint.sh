#!/bin/sh

# This script is a replacement for the standard entrypoint script used in the keycloak image.
# It expects that there is a directory from the host mounted at `/tmp/import` containing the `ssm.template` file.
# It expects that the `KEYCLOAK_CREDENTIALS_SECRET` environment variable is set to the value of the shared secret used to communicate with the system-modeller service.

# The `sed` command copies `ssm.template` to `ssm.json` while replacing the string "KEYCLOAK_CREDENTIALS_SECRET" with the value of the environment variable of the same name.
# Then the `kc.sh` command is run with the following arguments:
# --import-realm: causes Keycloak to import any JSON files found at /opt/keycloak/data/import (see bind mount below)
# --http-relative-path /auth: sets the Keycloak context path to be /auth (this was the default before Keycloak 17)

mkdir -p /opt/keycloak/data/import/
sed -e s/KEYCLOAK_CREDENTIALS_SECRET/${KEYCLOAK_CREDENTIALS_SECRET}/ /tmp/import/ssm.template > /opt/keycloak/data/import/ssm.json
/opt/keycloak/bin/kc.sh start-dev --import-realm --http-relative-path /auth
