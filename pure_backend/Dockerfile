# syntax=docker/dockerfile:1.7
FROM --platform=linux/arm64 maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
ENV HTTP_PROXY=${HTTP_PROXY} HTTPS_PROXY=${HTTPS_PROXY} NO_PROXY=${NO_PROXY}

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -Dmaven.test.skip=true \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=25 \
    clean package

FROM --platform=linux/arm64 eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/practice-platform-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
