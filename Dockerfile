# Stage 1

FROM maven:3.9.6 AS maven

WORKDIR /usr/src/app
COPY . /usr/src/app
RUN head -20 pom.xml  # print first 20 lines of file

RUN mvn clean install -Pdev -Dmaven.test.skip
RUN mvn -DskipTests package

# Stage 2

FROM openjdk:21-slim AS jdk

WORKDIR /opt/app

COPY --from=maven /usr/src/app/pom.xml /opt/app/
COPY --from=maven /usr/src/app/target/solarapi-*.jar /opt/app/solarapi.jar

EXPOSE 80

ENTRYPOINT ["java","-jar","solarapi.jar"]