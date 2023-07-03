#!/bin/bash
#
# This software is distributed WITHOUT ANY WARRANTY, without even the implied
# warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, except where
# stated in the Licence Agreement supplied with the software.
#
# Received by Panos Melas, University of Southampton IT Innovation Centre, in
# good faith on 28/04/2023 from
# https://stackoverflow.com/questions/53301299/add-healthcheck-in-keycloak-docker-swarm-service

# This is a workaround script for the missing cURL command not included in
# newer keycloak docker images. The script checks the health status of the
# keycloak service at http://localhost:8080/auth/health/ready
# {
#    "status": "UP",
#    "checks": [
#    ]
# }
#

exec 3<>/dev/tcp/localhost/8080

echo -e "GET /auth/health/ready HTTP/1.1\nhost: localhost:8080\n" >&3

timeout --preserve-status 1 cat <&3 | grep -m 1 '"status":\s\+"UP"'
ERROR=$?

exec 3<&-
exec 3>&-

exit $ERROR
