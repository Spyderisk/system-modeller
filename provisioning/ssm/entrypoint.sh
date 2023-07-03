#!/bin/sh

# This script is a replacement for the standard entrypoint script used in the
# catalina image. It expects that there is a directory from the host mounted at
# `/tmp/startup` containing the `entrypoint.sh` file.
#

#https://github-registry-files.githubusercontent.com/619830179/a8341180-decd-11ed-8428-25c0158a373b?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIWNJYAX4CSVEH53A%2F20230530%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20230530T140817Z&X-Amz-Expires=300&X-Amz-Signature=66ef5deb95184f3eda6b0c13f1043a870403d62964b39f10363512bc6201d640&X-Amz-SignedHeaders=host&actor_id=0&key_id=0&repo_id=619830179&response-content-disposition=filename%3Ddomain-network-6a3-1-1.zip&response-content-type=application%2Foctet-stream

wget https://github.com/SPYDERISK/domain-network/packages/1826148 -O /tmp/knowledgebases/domain-network.zip

echo "entrypoint: starting up catalina"
/var/lib/tomcat/bin/catalina.sh run
