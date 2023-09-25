#
# Build stage
#
FROM maven:3.8.6-jdk-8-slim AS build
COPY src /app/src
COPY pom.xml /app
#COPY maven_ali_mirror_settings.xml /usr/share/maven/ref/
#RUN mvn -s /usr/share/maven/ref/maven_ali_mirror_settings.xml -f /app/pom.xml clean package
RUN mvn  -f /app/pom.xml clean package
#
# Package stage
#
FROM jrottenberg/ffmpeg:4.1-ubuntu

COPY --from=build /app/target/HeiMusic-0.0.1-SNAPSHOT.jar /app/heimusic-server.jar
RUN apt-get update && apt-get -y --no-install-recommends install openjdk-8-jre-headless
EXPOSE 9001
ENTRYPOINT ["java","-jar","/app/heimusic-server.jar"]
