FROM openjdk:8-jre-alpine
MAINTAINER Rein Krul <reinkrul@gmail.com>

ENTRYPOINT ["/usr/bin/java", "-jar", "/jms-gateway/jms-gateway.jar"]

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
# ADD target/lib           /usr/share/myservice/lib
# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /jms-gateway/jms-gateway.jar