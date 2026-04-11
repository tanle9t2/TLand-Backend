# ── Build Stage ──────────────────────────────────────────────────────────────
# protobuf-maven-plugin tự gen Java gRPC stubs từ các file *.proto trong src/main/proto
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests

COPY src ./src
RUN mvn -B -q -DskipTests package

# ── Runtime Stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 1001 -S appgroup && adduser -S appuser -u 1001 -G appgroup

WORKDIR /app
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

USER 1001

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# HTTP REST
EXPOSE 8086
# gRPC server
EXPOSE 9195

HEALTHCHECK --interval=30s --timeout=10s --start-period=25s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8086/payment-service/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
