# Spyderisk System Modeller

The Spyderisk System Modeller (SSM) provides a thorough risk assessment of
complex systems making use of context and connectivity to take into account the
web of attack paths and secondary threat cascades in a system.

Spyderisk assists the user in following the risk assessment process defined in
ISO 27005 and thus supports the Information Security Management System defined
in ISO 27001. The Spyderisk System Modeller is a generic risk assessment tool
and must be configured with a model of a domain ("knowledgebase"), containing
the available asset types and relations, descriptions of the threats, the
possible security controls, and more.

The Spyderisk software does not come bundled with any particular knowledgebase;
this is configurable at build/deploy time, by putting one or more zip bundles
into the "knowledgebases" folder (described in more detail later). An example
knowledgebase has been developed for complex networked information systems,
which is available here:
https://github.com/Spyderisk/domain-network/packages/1826148

The web-based graphical user interface guides the user through the following
steps:

1. The user draws a model of their system model by dragging and dropping typed
   assets linked by typed relations onto a canvas.
2. The software analyses the model, inferring network paths, data flows,
   client-service trust relationships and much more (depending on the
knowledgebase).
3. The software analyses the model to find all the threats and potential
   controls that are encoded in the knowledgebase. The threats are
automatically chained together via their consequences to create long-reaching
and inter-linked attack graphs and secondary threat cascades through the
system.
4. The user assigns impact levels to various failure modes on the primary
   assets only.
5. The user can add controls to the model to reduce the likelihood of threats.
6. The software does a risk analysis, considering the external environment, the
   defined impact levels, the controls, and the chains of threats that have
been discovered. The threats and consequences may then be ranked by their risk,
highlighting the most important problems.
7. The user can choose to add or change the controls (back to step 5), to
   redesign the system (step 1), or to accept the system design.
8. The software can output reports describing the system along with the
   threats, consequences and their risk levels.

The knowledgebase describes threats through patterns of multiple assets along
with their context (such as network or physical location), rather than assuming
that threats relate to a single asset type. Similarly, methods to reduce threat
likelihood ("control strategies") may comprise multiple controls on different
assets (for example, both an X509 certificate at a service and verification of
the certificate at the client). Knowledgebases may also be designed such that
control strategies help solve one problem but exacerbate another (for example,
adding a password reduces the likelihood of unauthorised access to a service
but increases the likelihood of the legitimate user failing to get in). All
this provides a high degree of realism to the analysis.

With a compatible knowledgebase, the software can perform a both long-term risk
assessment suitable for when designing a system, and an operational (or
"runtime") risk assessment using a short time horizon. Different controls are
appropriate in each case (for instance, implementing a new staff security
training policy does not help with an ongoing attack, but blocking a network
path does). For the operational risk assessment, the state of the system model
must first be synchronised with the current operational state (for instance
through integration via the API with OpenVAS or a SIEM).

This project provides both a web service and a web-based user interface. An
API is provided to create, update, analyse and query system models and
integrate other tools.

Docker is used to provide a consistent build and test environment for
developers and for the continuous integration (CI) system.  If you want to do a
demo of the Spyderisk System Modeller and do not need to do any development
then you need to refer to the [Installing Docker](#installing-docker) section
and then use the separate "system-modeller-deployment" project.

Development of the software began in 2013, drawing on research dating back to
2008. It was open-sourced in early 2023. The research and development up to the
point of open sourcing was done solely by the [University of Southampton IT
Innovation Centre](http://www.it-innovation.soton.ac.uk/) in a variety of UK
and EU research projects.

## Pre-requisites

You will need `git`, `git-lfs`, `docker` and `docker-compose`. See below for
more detail.

## Quick Start

The following instructions assume that you have the pre-requisites installed
and working, and have cloned the repository.

N.B. Prior to running the following commands, you should also ensure that you
have one or more knowledgebases (domain models) available for installation.
These are available as zip file "bundles", containing the domain model itself,
along with the icons and mapping file needed for generating a UI palette of
visual assets.

An example knowledgebase is available at:
<https://github.com/Spyderisk/domain-network/packages/1826148> Here, you will
find the latest .zip bundle, at the bottom of the "Assets" list. This file
should be downloaded and copied into the system-modeller/knowledgebases folder.
Once Spyderisk has been started up (see instructions below), these zip files
will be automatically extracted and loaded into Spyderisk.

Of course, you may choose not to install a default knowledgebase, however, when
the Spyderisk GUI first loads in your browser, you will be directed to load in
a new knowledgebase manually.

1. `$ cd system-modeller`
2. `$ docker-compose up -d`
3. `$ docker-compose exec ssm bash`
4. `$ ./gradlew assemble bootTest`
5. Go to <http://localhost:8089> in your browser.
6. Login in using `testuser` or `testadmin` with password `password`.

N.B. Links in the user interface to documentation, the attack graph image, and
Keycloak account management functions do not currently work in the development
environment. Keycloak links can be corrected by editing the port in the URL to
be 8080.

Please also note that the default setup is to recreate all databases on initial
start-up. In order to persist any installed knowledgebases and created system
models, you should ensure that `reset.on.start=false` in your
`application.properties` file, prior to re-running `./gradlew assemble
bootTest`.

## Installing Docker

Please see the [Docker website](https://www.docker.com/) for details.

### Windows

To use Docker in Windows you must enable Hyper-V. This means that you can no
longer use VirtualBox (used as Vagrant's hypervisor).

Download and install "Docker Desktop".

#### With WSL2

Docker Desktop integrates with WSL2 (Windows Sub-system for Linux v2). WSL2
provides a Linux environment deeply integrated into Windows. As many
development tools are designed for Linux, using WSL2 can make things easier.
Docker provide [instructions for the Docker Desktop WLS2
backend](https://docs.docker.com/docker-for-windows/wsl/) which should be
followed.

As part of the WSL2 installation, [you choose and install a Linux distribution
to
use](https://docs.microsoft.com/en-us/windows/wsl/install-win10#step-6---install-your-linux-distribution-of-choice).

Once WSL2 and Ubuntu are installed, open a terminal window of some sort and
type `wsl` to switch to your default WSL2 Linux distro. You will need to copy
your private SSH key into the `.ssh` folder in the distro. You can access your
`C:` drive with the path `/mnt/c`:

```shell
wsl
cd
mkdir .ssh
chmod 700 .ssh
cp /mnt/c/path/to/your/key/id_rsa .ssh
chmod 600 .ssh/id_rsa
```

You may also want to limit the host resources that Docker Desktop is permitted
to use. This can be done with a `.wslconfig` file in your `%UserProfile`
folder, e.g.:

```
[wsl2]
memory=10GB
processors=8
```

At this point you have a functional Linux system. Please skip to the Linux
sub-section for the rest of the instructions.

#### Without WSL2

If you are not using WSL2, you will have to permit Docker Desktop to access the
location on your disk where you have the system-modeller cloned. Either (in
advance) add a file-share for "C:\" in the Docker Desktop UI or be more
specific to the area of the disk where the system-modeller is checked out.
Alternatively, wait for Docker Desktop to pop up a request for file sharing
when you execute the compose file.

You must also configure resource usage in the Docker Desktop UI. Configure it
to:

* have more memory, CPU, swap (e.g. all CPUs, 8GB memory, 2GB swap);

### Linux

Many Linux distributions already have Docker installed. The following command
will work in `apt` based systems such as Ubuntu. To install Docker:

```shell
sudo apt-get install docker docker-compose
```

## Docker Concepts

Docker manages individual containers and its configuration files by default are
called "Dockerfile". It is possible to manually orchestrate (and connect)
docker containers but it is easier to manage multiple containers using
"docker-compose". Docker Compose files by default are called
"docker-compose.yml" and they refer to docker images (either to be pulled from
a docker registry) or to local Dockerfiles.

### Images

Images are the equivalent of VM images. They are built in layers on top of
standard distributions (e.g. from Docker hub). Names and tags (e.g.
"postgres:9.6.19" or "mongo:latest"). Be careful with the "latest" tag as use
of it does not guarantee that you are actually getting the latest version - it
is just a string like any other. By convention it will be the latest but
perhaps not if it is retrieved from your local cache.

Commands:

* List all local images with `docker image ls`

### Containers

Containers are running instances of images. They can be paused, unpaused,
stopped and started once created or just destroyed and recreated. The state of
a container changes as it runs with processes writing to disk and updating
memory. Writing to disk creates a new layer in the image by default or changes
a persistent "volume" if it is defined (see below). If a container is paused
then all the processes are paused (with `SIGSTOP`) and can be resumed but if a
container is stopped (`SIGTERM` then `SIGKILL`) then the memory state is lost.

Commands:

* List containers that relate to the local `docker-compose.yml` file with
  `docker-compose ps`
* List running containers with `docker container ls` or just `docker ps`
* List all containers with `docker container ls -a` or just `docker ps -a`
* Remove the containers that relate to the local `docker-compose.yml` file with
  `docker-compose rm`
* Remove a container with `docker container rm <container ID>`
* Remove containers that are not running with `docker container prune` (be
  careful!)

### Volumes

There are two main sorts of volume:

1. where a folder from the host is mounted in the container (a "bind mount");
2. where a volume is created in a private space used by Docker. There are
   "named volumes" and "anonymous volumes" of this type.

We use (1) to mount the source code from the host into the container so that
editing can be done on the host and building in the container. Volume type (2)
is used for folders in the container where the contents changes such as the
`build` folder and gradle cache. All volumes are persisted separately to the
container. Anonymous volumes are given enormous random identifiers so they are
hard to identify. Named volumes can be shared between two different executing
containers and can be reused between container instantiations. Anonymous
volumes are not reused, they are left as orphans when a container is destroyed.

Named volumes for the databases are defined in `docker-compose.yml`. They are
called `jena`, `mongo-db` and `mongo-configdb`. When docker-compose creates the
volumes, it prefixes those names with the "project name" which by default is
the name of the folder containing the `docker-compose.yml` file. Therefore the
volume names are likely to be e.g. `system-modeller_jena`.

**N.B. if you have two system-modeller folders with the same name then they
will end up using the same named volumes: this is almost certainly not what you
want.**

Named volumes for build artifacts are defined in the
`docker-compose.override.yml` file. They cover the `gradle`, `npm` and `maven`
artifact folders.

Commands:

* List all volumes with `docker volume ls`
* Remove a volume with `docker volume rm <volume ID>`
* Remove all volumes that are not used by a container (running or not) with
  `docker volume prune` (it is sensible to do this periodically to save disk
space)

### Networks

Networks provide communication between containers.

### Container registry

Images (not containers actually) are stored in "registries". There is a cache
for images on your local machine and there are remote registries. We use Docker
Hub for standard images.

## Use of Docker

Docker: manages single containers. We have a multi-stage `Dockerfile` which
defines three different containers for the SSM:

* The `ssm-dev` container holds the development environment (including Java,
  Gradle, Maven, etc). It is the one used by developers who will be building
the SSM themselves with `gradle` commands. The basic strategy is to keep the
source code files on the host and mount them into the SSM container. In this
way, the build tools are isolated from the host but the source code can still
be edited easily in any editor on the host.
* The `ssm-build` container executes a clean build of the SSM and is used by
  the CI system.
* The `ssm-production` container is created by the CI and is a light-weight
  container with just the software necessary for executing the SSM. It can be
used for demos and is intended for "production" deployment. The "production"
image is built on any branch that the CI builds (e.g. master and dev). The
"production" image is used in the separate "system-modeller-deployment"
project.

Docker-compose: orchestrates multiple containers. We use several files:

* The `docker-compose.yml` defines the base orchestration of the SSM
  development container with supporting services (e.g. a MongoDB container).
* The `docker-compose.override.yml` file is (by default) overlayed on top of
  `docker-compose.yml` and adds in the development environment.
* The `docker-compose.test.yml` file is used (primarily by the CI pipeline) to
  execute the integration tests.

## Initialise for Development

### Install git and git-lfs

On an Ubuntu system:

```shell
sudo apt-get update
sudo apt-get install git git-lfs
```

### Run an SSH Agent

You should be using an SSH key to authenticate with GitLab. To avoid typing in
the private key password all the time, you should run an SSH agent which holds
the unencrypted private key in memory. In Windows you can use e.g. `pageant`
(part of Putty). In Linux (or WSL2) do:

```shell
eval `ssh-agent`
ssh-add
```

### Clone the system-modeller Git Repository

Cloning the `system-modeller` repository makes a copy of all the files (and
their history) on your local machine. If you are using WSL2 then you should
clone the repository within your chosen Linux distribution.

```shell
git clone git@github.com:SPYDERISK/system-modeller.git
cd system-modeller
```

### Customise default Configuration Parameters (Optional Step)

The default configuration of the Spyderisk service, including service ports and
credentials, can be customized through the '.env' file. To get started, please
make a copy of the provided `.env.template` file and rename it to `.env`. Then,
you can modify any of the default parameters in the `.env` file to match your
specific environment configuration.

### Download and Install default Knowledgebase(s)

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


### Starting the Containers

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

### Getting a Shell

To get a shell in the `ssm` container:

```shell
docker-compose exec ssm bash
```

The equivalent `docker` command requires the full container name and also the
`-it` flags to attach an interactive terminal to the process, e.g.:

```shell
docker exec -it system-modeller_ssm_1 bash
```

### Viewing logs

To see the logs from a service and `tail` the log so that it updates, the
command is:

```shell
docker-compose logs -f <SERVICE>
```

Where `<SERVICE>` could be e.g. `ssm`.

### Port Mappings

The various server ports in the container are mapped by Docker to ports on the
host. The default ports on the host are defined in `docker-compose.yml` and `docker-compose.override.yml`:

* 3000: Nodejs (3000) on the `ssm` container 
* 5005: Java debugging (5005) on the `ssm` container
* 8081: Tomcat (8081) on the `ssm` container
* 8080: Keycloak (8080) on the `keycloak` container
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

## Development

The system-modeller source code is synchronised with the `ssm` container. This
means that you can use your favourite source code editor on your host machine
but still do the build and execution inside the `ssm` container. The
system-modeller folder is mounted at `/code` inside the `ssm` container.

Other folders which are written to by the build/execution such as `build`,
`logs`, `jena-tdb` are not mounted from the host for performance reasons. They
may only easily be accessed from within the container.

### Gradle Tasks

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

### Keycloak

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

### Frontend Development

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

#### Debugging the Frontend

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

### Backend Development

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

#### Debugging the Backend

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

### Shutdown and Persistence

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


### Building a Spyderisk System Modeller Image

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

## Licences

The [license finder](https://github.com/pivotal/LicenseFinder) software should
be used to find the licences of 3rd-party code. It is not installed in the dev
image by default.

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

## OpenAPI

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

Note that the object fields `aLabel`, `mLabel` and `rLabel` used in
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
