# =============================================================================
# ai-service - Multi-Stage-Build (kein lokales Java/Maven noetig)
#   Stufe 1: mit Maven bauen   Stufe 2: schlankes JRE-Image
# Bauen:   docker build -t werkmundo/ai-service .
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /deployments
COPY --from=build /build/target/quarkus-app/lib/ ./lib/
COPY --from=build /build/target/quarkus-app/*.jar ./
COPY --from=build /build/target/quarkus-app/app/ ./app/
COPY --from=build /build/target/quarkus-app/quarkus/ ./quarkus/
RUN addgroup -S app && adduser -S app -G app
USER app
ENV HTTP_PORT=8086
EXPOSE 8086
HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:${HTTP_PORT}/q/health/ready || exit 1
ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", "-jar", "quarkus-run.jar"]
