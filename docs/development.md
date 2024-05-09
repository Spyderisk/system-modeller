# Development

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

