# Spyderisk development

This document is about development on the Spyderisk web application and backend
service, written in Java (and Javascript, of course.) It is not for development
using the Spyderisk Python adaptor, and it is not for development of Spyderisk
domain models.

# Contents

* [Initialise for Development](#initialise-for-development)
    * [Install git and git-lfs](#install-git-and-git-lfs)
    * [Run an SSH Agent](#run-an-ssh-agent)
    * [Clone the system-modeller Git Repository](#clone-the-system-modeller-git-repository)
    * [Customise default Configuration Parameters (Optional Step)](#customise-default-configuration-parameters-(optional-step))
    * [Download and Install default Knowledgebase(s)](#download-and-install-default-knowledgebase(s))
    * [Starting the Containers](#starting-the-containers)
    * [Getting a Shell](#getting-a-shell)
    * [Viewing logs](#viewing-logs)
    * [Port Mappings](#port-mappings)
* [Spyderisk application development](#spyderisk-application-development)
    * [Gradle Tasks](#gradle-tasks)
    * [Keycloak](#keycloak)
    * [Frontend Development](#frontend-development)
        * [Debugging the Frontend](#debugging-the-frontend)
    * [Backend Development](#backend-development)
        * [Debugging the Backend](#debugging-the-backend)
    * [Shutdown and Persistence](#shutdown-and-persistence)
    * [Building a Spyderisk System Modeller Image](#building-a-spyderisk-system-modeller-image)
* [OpenAPI](#openapi)
* [License checks](#license-checks)
    * [The LicenseFinder tool](#the-licensefinder-tool)
        * [Installation](#installation)
        * [Usage](#usage)

# Initialise for Development

## Install git and git-lfs

On an Ubuntu system:

```shell
sudo apt-get update
sudo apt-get install git git-lfs
```

## Run an SSH Agent

You should be using an SSH key to authenticate with GitLab. To avoid typing in
the private key password all the time, you should run an SSH agent which holds
the unencrypted private key in memory. In Windows you can use e.g. `pageant`
(part of Putty). In Linux (or WSL2) do:

```shell
eval `ssh-agent`
ssh-add
```

## Clone the system-modeller Git Repository

Cloning the `system-modeller` repository makes a copy of all the files (and
their history) on your local machine. If you are using WSL2 then you should
clone the repository within your chosen Linux distribution.

```shell
git clone git@github.com:SPYDERISK/system-modeller.git
cd system-modeller
```

## Customise default Configuration Parameters (Optional Step)

The default configuration of the Spyderisk service, including service ports and
credentials, can be customized through the '.env' file. To get started, please
make a copy of the provided `.env.template` file and rename it to `.env`. Then,
you can modify any of the default parameters in the `.env` file to match your
specific environment configuration.

## Download and Install default Knowledgebase(s)

Syderisk requires one or more knowledgebase (domain model) to be installed,
prior to being able to develop system models in the GUI. These are available as
zip file "bundles", containing the domain model itself, along with the icons
and mapping file needed for generating a UI palette of visual assets.

An example knowledgebase is available at:
https://github.com/Spyderisk/domain-network/packages/1826148 Here, you will
find the latest .zip bundle, at the bottom of the "Assets" list. This file
should be downloaded and copied into the system-modeller/knowledgebases folder.
Once Spyderisk has been started up (i.e. via starting the containers), these
zip files will be automatically extracted and loaded into Spyderisk.

Of course, you may choose not to install a default knowledgebase, however, when
the Spyderisk GUI first loads in your browser, you will be directed to load in
a new knowledgebase manually.

## Starting the Containers

To optimise the build, configure Docker to use "buildkit":

```shell
export DOCKER_BUILDKIT=1
```

To bring the containers (ssm, mongo, keycloak) up and leave the terminal
attached with the log files tailed:

```shell
docker-compose up
```

Alternatively, to bring the containers up and background (detach) the process:

```shell
docker-compose up -d
```

The `docker-compose.yml` file does not set the `container_name` property for
the containers it creates. They therefore get named after the directory
containing the `docker-compose.yml` file (the "project name") along with the
identifier in the `docker-compose.yml` file and a digit (for uniqueness). The
directory containing the `docker-compose.yml` file will, by default, be called
`system-modeller` as that is the default name when doing `git clone`. Docker
Compose picks up this name and uses it as the "project name". If more than one
instance of the SSM is required on one host, an alternative project name is
needed: either by renaming the `system-modeller` folder (recommended) or by
using the `-p` flag in `docker-compose` (e.g. `docker-compose -p <project name>
up -d`) but you must remember to use this flag every time.

## Getting a Shell

To get a shell in the `ssm` container:

```shell
docker-compose exec ssm bash
```

The equivalent `docker` command requires the full container name and also the
`-it` flags to attach an interactive terminal to the process, e.g.:

```shell
docker exec -it system-modeller_ssm_1 bash
```

## Viewing logs

To see the logs from a service and `tail` the log so that it updates, the
command is:

```shell
docker-compose logs -f <SERVICE>
```

Where `<SERVICE>` could be e.g. `ssm`.

## Port Mappings

The various server ports in the container are mapped by Docker to ports on the
host. The default ports on the host are defined in `docker-compose.yml` and `docker-compose.override.yml`:

* 3000: Nodejs (3000) on the `ssm` container 
* 5005: Java debugging (5005) on the `ssm` container
* 8080: Keycloak (8080) on the `keycloak` container
* 8081: Tomcat (8081) on the `ssm` container
* 8089: Nginx (80) on the `proxy` container

To change the ports mapping it is best to copy the `.env.template` file to `.env` and define the port numbers there. This is necessary if you need to run multiple instances of the service on the same host.

The Nginx reverse proxy forwards requests to the appropriate container and also includes redirects for documentation links. Therefore, it is advised to use port 8089 

*The rest of this document assumes the default port mapping.*

To see the containers created by the `docker-compose` command along with their
ports:

```shell
$ docker-compose ps
NAME                         IMAGE                     COMMAND                  SERVICE             CREATED             STATUS              PORTS
system-modeller-proxy-1      nginx:stable-alpine3.17   "/tmp/import/entrypo…"   proxy               23 minutes ago      Up 23 minutes       0.0.0.0:8089->80/tcp
system-modeller-keycloak-1   keycloak/keycloak:21.0    "/tmp/import/entrypo…"   keycloak            23 minutes ago      Up 23 minutes       0.0.0.0:8080->8080/tcp, 8443/tcp
system-modeller-mongo-1      mongo:5.0.16-focal        "docker-entrypoint.s…"   mongo               23 minutes ago      Up 23 minutes       27017/tcp
system-modeller-ssm-1        system-modeller-ssm       "tail -f /dev/null"      ssm                 23 minutes ago      Up 23 minutes       0.0.0.0:3000->3000/tcp, 0.0.0.0:5005->5005/tcp, 0.0.0.0:8081->8081/tcp```

You might contrast that with a list of all containers on the host found through
the `docker` command:

```shell
$ docker ps
CONTAINER ID   IMAGE                     COMMAND                  CREATED          STATUS          PORTS                                                                    NAMES
01cc2804cadf   nginx:stable-alpine3.17   "/tmp/import/entrypo…"   24 minutes ago   Up 24 minutes   0.0.0.0:8089->80/tcp                                                     system-modeller-proxy-1
0a91f360c30b   system-modeller-ssm       "tail -f /dev/null"      24 minutes ago   Up 24 minutes   0.0.0.0:3000->3000/tcp, 0.0.0.0:5005->5005/tcp, 0.0.0.0:8081->8081/tcp   system-modeller-ssm-1
1b27ac53ec18   keycloak/keycloak:21.0    "/tmp/import/entrypo…"   24 minutes ago   Up 24 minutes   0.0.0.0:8080->8080/tcp, 8443/tcp                                         system-modeller-keycloak-1
a67ba45f70c5   mongo:5.0.16-focal        "docker-entrypoint.s…"   24 minutes ago   Up 24 minutes   27017/tcp                                                                system-modeller-mongo-1
```

# Spyderisk application development

The system-modeller source code is synchronised with the `ssm` container. This
means that you can use your favourite source code editor on your host machine
but still do the build and execution inside the `ssm` container. The
system-modeller folder is mounted at `/code` inside the `ssm` container.

Other folders which are written to by the build/execution such as `build`,
`logs`, `jena-tdb` are not mounted from the host for performance reasons. They
may only easily be accessed from within the container.

## Gradle Tasks

The main `build.gradle` file has a few tasks defined as well as the standard
ones:

* assemble: builds the WAR including compiling Java and bundling JS
* test: compiles the Java and runs the tests (`classes` and `testClasses`)
* build: does assemble and also does test
* bootDev: does Spring's `bootRun` task with the profile set to `dev` and
  without any dependencies running
* bootTest: does Spring's `bootRun` task with the profile set to `test` and
  building the webapp first
* `gradle :taskTree :<task>` shows what `<task>` will do (use `--no-repeat` to
  remove repeated tasks)

There is also a `build.gradle` in `src/main/webapp` for the web application. It
mostly runs `yarn` commands via the gradle yarn plugin (yarn itself is not
directly installed and available).

As yarn is not available directly, to add or remove packages during development
use commands such as:

* `gradle addPackage -P packName="react-dom@4.6.6"`
* `gradle addPackage -P packName="lodash" -P dev="true"`
* `gradle removePackage -P packName="react-dom"`
* `gradle install`

After a commit that has changed the contents of `src/main/webapp/package.json`,
a `gradle install` is necessary to update the local cache. This runs a `yarn
install` which removes any unnecessary packages and installs the packages in
`package.json`, in addition to any new additions.  `gradle build` only rebuilds
the cache of webapp from scratch if a new clean environment is found.

To create the webapp environment from scratch, follow the steps below:

1. `cd src/main/webapp`
2. `gradle clean`
3. `rm -rf src/main/webapp/node_modules`
4. `rm -rf src/main/webapp/.gradle`
5. `gradle install`

## Keycloak

The development environment initialises an *insecure* Keycloak service. The
Keycloak configuration is stored in `provisioning/keycloak/ssm.json` and:

* creates a realm (`ssm-realm`) within which there is a `user` and `admin` role
  defined;
* permits holders of the `admin` role to manage the realm's users;
* creates a client (`system-modeller`) and uses the
  `KEYCLOAK_CREDENTIALS_SECRET` environment variable (defined in `.env`) to
insert a shared secret for communication with the system-modeller service;
* creates a user called `testuser` holding the `user` role, with password
  `password`;
* creates a user called `testadmin` holding the `admin` role, with password
  `password`.

The Keycloak (master realm) administrator username and password is also defined
in `.env` and is admin/password.

## Frontend Development

[Get a shell](#getting-a-shell) on the `ssm` container, build the code and
start the backend server on port 8081 <http://localhost:8081/system-modeller/>:

```shell
docker-compose exec ssm bash
cd /code
./gradlew build
./gradlew bootDev
```

This starts a Tomcat servlet which handles API requests and also handles
requests for the simple HTML pages. Using `bootDev` is the same as doing
`./gradlew bootRun` but sets `spring.profiles.active` to `dev` which means that
the properties from `src/main/resources/application-dev.properties` are
overlayed on the standard property file. This is defined in the `bootDev`
target of `build.gradle`. Note that whereas `bootRun` would compile, `bootDev`
does not.

The command does not exit until you press Ctrl-C at which point the server is
stopped. If necessary the backend can be recompiled with `./gradlew test` or
just `./gradlew classes` and the server started again with `./gradlew bootDev`.

If `application.properties` changes then `./gradlew assemble` is needed to get
it into the webapp.

[Get another shell](#getting-a-shell) on the `ssm` container and start the
frontend server on port 3000 (which will be used by e.g. the dashboard and
modeller pages):

```shell
docker-compose exec ssm bash
cd /code
./gradlew start
```

Note that this gradle target is defined in `src/main/webapp/build.gradle`. It
starts the server defined in `src/main/webapp/server.js` which uses the Express
framework on top of NodeJS to serve the part of the SSM with the canvas in (the
main part). It proxies requests for other pages through to the Spring Java
backend.

The command does not exit until you press Ctrl-C but upon doing so the NodeJS
server continues executing. There is another gradle task `./gradlew stopNode`
which kills all node processes.

When running this NodeJS server, webpack compile events are listened for and
the client web page is automatically updated. Sometimes reloading the page in
browser is needed, but normally the hot reload works fine.

Note: the ports 8081 and 3000 are hard-coded into the Express `server.js` file.
Any change to the port mapping needs to be reflected there.

If, when running `./gradlew start` you get an error message about `Error:
listen EADDRINUSE: address already in use 0.0.0.0:3000` or similar, it is
likely that Node is already running. You might want to stop it and start it
again with `./gradlew stopNode start`.

### Debugging the Frontend

It is recommended that you install the following plugins in Chrome (or similar
browser):

* React Developer Tools: shows the React component hierarchy and each
  component's (editable) state and props.
* Redux DevTools: show the application state, including how it changes with
  events

In VSCode (for instance), the following configuration in `.vscode/launch.json`
will enable debugging of the frontend from within VSCode (launch with F5):

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "pwa-chrome",
            "request": "launch",
            "name": "Launch Chrome against localhost:3000 for frontend dev",
            "url": "http://localhost:3000/system-modeller",
            "webRoot": "${workspaceFolder}/src/main/webapp",
        }
    ]
}
```

## Backend Development

If the main web UI is not being changed then it is simpler not to run the
NodeJS server.

Get a shell on the ssm container (see above).

Build the code and start the backend server:

```shell
docker-compose exec ssm bash
cd /code
./gradlew build
./gradlew bootTest
```

The bootTest target sets `spring.profiles.active` to `test` but it is not clear
that this has any effect (TODO). It also bundles the Javascript webapp and then
extracts the files. Finally it runs the `./gradlew boot` task which starts a
Tomcat servlet. As a result the whole SSM application works but the frontend is
served from static files that are not hot-reloaded.

The SSM served by Tomcat can be accessed at
<http://localhost:8089/system-modeller> (via the proxy) or direct to Tomcat via port 8081.

### Debugging the Backend

Add the flag `--debug-jvm` to any of the usual `gradle` commands and the JVM
will wait for a debugger to connect on guest port 5005. It is the equivalent of
adding `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005` to
the JVM command line.

```shell
./gradlew bootTest --debug-jvm
```

Then connect to `localhost:5005` from your IDE.

In VSCode, for instance, debugger connections are configured in
`.vscode/launch.json`. The necessary configuration is:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Attach to Java debugger on localhost:5005 for backend dev",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        }
    ]
}
```

## Shutdown and Persistence

The containers can be paused (and unpaused) which pauses the processes inside
the container and thus releases host resources but does not lose process state:

```shell
docker-compose pause
docker-compose unpause
```

The containers can be stopped (and started) which will kill all the processes
running in the container but leave the container present:

```shell
docker-compose stop
docker-compose start
```

If you originally used `docker-compose up` to start the containers without
detaching (with `-d`) then `Ctrl-C` is the same as `docker-compose stop`.

The `docker-compose down` command stops the containers, removes them and
removes the networks they were using. There are also optional parameters to
remove the volumes and images:

```shell
docker-compose down
```

In all these cases, the (Docker disk) volumes are persisted and named volumes
will be reattached to new containers, during restart. Assuming that you have
`reset.on.start=false` in your `application.properties` file, this also means
that any knowledgebases (domain models), system models, palettes, etc will be
persisted after restarting the containers.

If the intention is to recreate the databases or reinstall the default
knowledgebases, this may be done in the following ways:

a) Use `docker-compose down -v`, then restart containers and Spyderisk as
normal, e.g.

```shell
docker-compose down -v
docker-compose up -d
docker-compose exec ssm bash
./gradlew assemble bootTest
```

b) Leave containers running, but set `reset.on.start=true` in your
`application.properties` file, then restart Spyderisk, e.g.

```shell
docker-compose exec ssm bash
./gradlew assemble bootTest
```


## Building a Spyderisk System Modeller Image

Sometimes, to test something you need to build a "production" image of the sort
built by the CI pipeline. You can then for instance use the image in the
`system-modeller-deployment` project.

To build a production image use something like:

`docker build --tag my-ssm-image --build-arg BUILDKIT_INLINE_CACHE=1 --file Dockerfile --target ssm-production .`

If you need to test the image in the `system-modeller-deployment` project then
just edit the `docker-compose.yml` file in that project to reference
`my-ssm-image` instead of the image held remotely, e.g.:

```yaml
  ssm:
    image: my-ssm-image:latest
```

When you're done with the image, remove it with `docker image rm my-ssm-image`.

# OpenAPI

The OpenAPI v3 documentation is automatically generated from the Java service
code and is available from a live service at

* `http://server:port/context-path/v3/api-docs`, e.g.
  `/system-modeller/v3/api-docs` (for JSON)
* `http://server:port/context-path/v3/api-docs.yaml`, e.g.
  `/system-modeller/v3/api-docs.yaml` (for YAML)

The Swagger UI is also available for browsing the API:

* `http://server:port/context-path/swagger-ui.html`, e.g.
  `/system-modeller/swagger-ui.html`

The file [openAPI-3-schema.YAML](docs/openapi/openAPI-3-schema.YAML) in this
repository is created by hand by combining the autogenerated YAML file along
with the first few lines of the existing file.

Note that the object fields `aLabel`, `mLabelhelpful tools ` and `rLabel` used in
`MisbehaviourSet` and `Node` are inconsistent between the OpenAPI file and the
JSON returned by the service. The OpenAPI file suggests they are all lower-case
but in the JSON they are camelCase (`aLabel` etc). To auto-generate effective
client code from the OpenAPI document it may be necessary to first replace
`alabel` with `aLabel` and so on.

Another change that may be necessary is to replace `date-time` with `int64`
where the following fragment is found:

```yaml
created:
    type: string
    format: date-time
```

# License checks

License compliance is a necessary part of software software development.

The Spyderisk Project takes responsibility for the license compliance of what
we ship. We use a large stack of npm/yarn code that changes without our
knowledge or permission, and there are different licenses within these stacks.
By default we believe what npm and yarn tell us:

```
$ npx license-checker --summary
$ yarn licenses list
```

However these are only as good as their inputs, and in any case explicitly
disclaim to be authoritative. There are many handy helper tools such as 
[npm License Tracker](https://github.com/amittkSharma/npm-license-tracker)
[Repository License Crawler](https://github.com/sinipelto/repo-license-crawler)
which will examine Python, npm etc application trees and give an opinion on which licenses are in use. 

We have documented experiments with one particular helper tool as follows to assist with the
[statements we make about licenses](../LICENSE.md). 

## The LicenseFinder tool

The [license finder](https://github.com/pivotal/LicenseFinder) software can 
generate some opinions about the licences of 3rd-party Javascript code.

### Installation

Install `license_finder`:

```shell
apt-get install ruby
gem install license_finder
```

To use `license_finder` in the `webapp` folder, `yarn` (and therefore `npm`) is
also required (rather than the versions built in to the `gradle` plugin):

```shell
apt-get install nodejs
apt-get install npm
npm install --global yarn
```

### Usage

Decisions on which licences are approved (and why) are kept in the top-level
`dependency_decisions.yml` file.

To find all licences and check them against the approved list:

```shell
cd /code
license_finder --decisions-file=/code/dependency_decisions.yml
cd /code/src/main/webapp
license_finder --decisions-file=/code/dependency_decisions.yml
```

To generate an HTML report, use the command:

```shell
license_finder report --format html --quiet --aggregate-paths /code /code/src/main/webapp --decisions-file=/code/dependency_decisions.yml > licences.html
```

To generate a CSV report, use the command:

```shell
license_finder report --quiet --aggregate-paths /code /code/src/main/webapp --decisions-file=/code/dependency_decisions.yml --columns name version authors licenses license_links approved homepage package_manager --write-headers > licences.csv
```


