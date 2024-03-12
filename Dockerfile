#DOCKER_BUILDKIT=0 docker build -t registry.api.liehr.app/domsapi:dev .
#https://medium.com/@skleeschulte/how-to-enable-ipv6-for-docker-containers-on-ubuntu-18-04-c68394a219a2
#https://blog.miyuru.lk/enabling-ipv6-on-maven-java/
#ip6tables -t nat -A POSTROUTING -s fd00::/80 ! -o docker0 -j MASQUERADE
#RUN mvn package --batch-mode --errors --fail-at-end --show-version

# Stage 1

FROM maven:3.9.6 AS maven

WORKDIR /usr/src/app
COPY . /usr/src/app
RUN head -20 pom.xml  # print first 20 lines of file

RUN mvn -DskipTests package

# Stage 2

FROM openjdk:21-slim AS jdk

WORKDIR /opt/app

COPY --from=maven /usr/src/app/pom.xml /opt/app/
COPY --from=maven /usr/src/app/target/solarapi-*.jar /opt/app/solarapi.jar

EXPOSE 80

ENTRYPOINT ["java","-jar","solarapi.jar"]