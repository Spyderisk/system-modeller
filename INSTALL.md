# Spyderisk System Modeller installation

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

N.B. The links in the user interface for the attack graph image does not work in the development environment. Links to Keycloak account management functions and documentation do not work via port 8081 but do work via port 8089.

Please also note that the default setup is to recreate all databases on initial
start-up. In order to persist any installed knowledgebases and created system
models, you should ensure that `RESET_ON_START=false` in your `.env` file, prior to re-running `./gradlew assemble
bootTest`.

## Installing Docker

Please see the [Docker website](https://www.docker.com/) for details.

### Windows

We assume WSL2 (Windows Subsystem for Linux v2) is installed. If you do not install WSL2,
you will need run Linux within a virtual machine, or switch to running Linux instead of Windows.

Download and install the closed-source application "Docker Desktop" for Windows.

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

### Linux

Many Linux distributions already have Docker installed. The following command
has been tested in `apt` based systems such as Ubuntu. To install Docker:

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

