ARG DOCKER_REGISTRY=
FROM ${DOCKER_REGISTRY}maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn/settings.xml /root/.m2/settings.xml

RUN mvn dependency:go-offline -DskipTests -Dmaven.repo.local=/root/.m2/repository -s /root/.m2/settings.xml

COPY src ./src

RUN mvn package -DskipTests -Dmaven.repo.local=/root/.m2/repository -s /root/.m2/settings.xml

ARG DOCKER_REGISTRY=
FROM ${DOCKER_REGISTRY}eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/device-management-1.0.0.jar app.jar

EXPOSE 8121

ENTRYPOINT ["java", "-jar", "app.jar"]
