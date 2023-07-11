#!/bin/sh

# This script is a replacement for the standard entrypoint script used in the
# nginx image. It expects that there is a directory from the host mounted at
# `/tmp/import` containing the `nginx.conf.template` file. It expects that the
# `SERVICE_PROTOCOL` and `SERVICE_PORT` environment variables are set in the
# `.env` file.

# The `endsubst` replaces `scheme` and `server_port` variables and copies
# `nginx.conf.template` to `nginx.conf` Then the `nginx` command is run the
# nginx service.

envsubst '$${scheme} $${server_port} $${kc_proxy_pass} $${documentation_url} $${tomcat_port}' < \
    /tmp/import/nginx.conf.template > /etc/nginx/nginx.conf

nginx -g 'daemon off;'
