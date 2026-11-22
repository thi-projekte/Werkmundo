FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /deployments

RUN addgroup -S app && adduser -S app -G app \
    && chown app:app /deployments

COPY --from=build --chown=app:app /build/target/quarkus-app/ ./

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]