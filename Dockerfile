# This is a multi-stage Docker build file.
# The stages are called "ssm-dev", "ssm-build" and "ssm-production".
# You can choose the stage using "--target <stage>" in a docker command. The default is the final stage.
#   ssm-dev: includes just the build tools we are using
#   ssm-build: copies in the source code and builds it (but does not test it)
#   ssm-production: copies the WAR file from ssm-build into a light-weight "alpine" container and executes Tomcat
# The ssm-dev container is used by developers along with the docker-compose.yml file (which brings in e.g. mongo)
# The ssm-production container can be used in production with the docker-compose-production.yml file (which brings in e.g. mongo)

#
# ssm-dev
#

FROM gradle:6.3.0-jdk8 AS ssm-dev
# gradle:6.3.0-jdk8 builds on adoptopenjdk:8-jdk-hotspot which builds on ubuntu:18.04
#TODO: use smaller image with specific versions of tools
#TODO: remove gradle from image now that we are using gradle wrapper

# LABELs are added to the image metadata
LABEL org.opencontainers.image.vendor="IT Innovation Centre"
LABEL org.opencontainers.image.title="Spyderisk System Modeller development image"

# Need gradle v6, java v8, python3 and python3-lxml (needed for jacoco2cobertura), killall (from psmisc)
RUN apt-get update && apt-get -y install python3 python3-lxml psmisc

WORKDIR /code

# This command ensures that the container continues running when started
CMD ["tail", "-f", "/dev/null"]

#
# ssm-build
#

FROM ssm-dev AS ssm-build

# The build arguments can be set using `--build-arg` in the docker command, or with "build:args" in docker-compose.
# They are not stored in the image environment.

# Maven credentials (e.g. GitHub username and a Github Personal Access Token able to read packages)
ARG MAVEN_USER
ARG MAVEN_PASS

LABEL org.opencontainers.image.title="Spyderisk System Modeller build image"

# Copy in only the files needed for the build: it's cleanest and it means more cache hits
COPY src /system-modeller/src/
COPY gradle /system-modeller/gradle/
COPY gradlew build.gradle settings.gradle /system-modeller/

# Build the software
# -P defines a "project property" available in build.gradle as a normal variable
RUN cd /system-modeller && ./gradlew clean assemble -PmavenUser=${MAVEN_USER} -PmavenPass=${MAVEN_PASS}

# Note: it is tempting to execute the tests here but we cannot do that as mongo and keycloak services are required

#
# ssm-production
#

FROM alpine:3.11 AS ssm-production

# The build arguments are set using `--build-arg` by the CI
# Build metadata
ARG CI_COMMIT_SHA
ARG CI_COMMIT_TIMESTAMP
ARG CI_RELEASE

LABEL org.opencontainers.image.vendor="IT Innovation Centre"
LABEL org.opencontainers.image.title="Spyderisk System Modeller"
LABEL org.opencontainers.image.revision=${CI_COMMIT_SHA}
LABEL org.opencontainers.image.created=${CI_COMMIT_TIMESTAMP}
LABEL org.opencontainers.image.release=${CI_RELEASE}

ENV SPRING_PROFILES_ACTIVE=production

# Install packaged dependencies
# mailcap includes /etc/mime.types which is currently used by the SSM for filetype recognition (e.g. ".gz")
RUN apk --no-cache add openjdk8 curl mailcap

# Download and install tomcat
RUN curl -s -o "tomcat.tar.gz" https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.38/bin/apache-tomcat-9.0.38.tar.gz
RUN apk del curl
RUN tar xfz tomcat.tar.gz -C /var/lib
RUN rm tomcat.tar.gz
RUN mv /var/lib/*tomcat* /var/lib/tomcat

# Copy in Tomcat config
COPY provisioning/tomcat/conf/server.xml /var/lib/tomcat/conf
COPY provisioning/tomcat/bin/setenv.sh /var/lib/tomcat/bin

# Copy in system-modeller WAR from ssm-build stage (it is unpacked when Tomcat runs)
COPY --from=ssm-build /system-modeller/build/build/libs/*.war /var/lib/tomcat/webapps/system-modeller.war

CMD ["/var/lib/tomcat/bin/catalina.sh", "run"]
