# Stage 1: Build stage
FROM maven:3.9.8-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Pre-fetch dependencies using pom.xml layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and package executable JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal, secure runtime stage
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Abhishek <abhishek@fintech.com>"
LABEL description="FintechLedger - Event-Driven Ledger & High-Throughput Wallet Engine"

WORKDIR /app

# Create a non-privileged system user and group for running the container
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from builder stage
COPY --from=builder /build/target/fintech-ledger-*.jar app.jar
RUN chown -R spring:spring /app

USER spring:spring

# Expose HTTP port
EXPOSE 8080

# Environment variables with sensible container defaults
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE="dev"

# Healthcheck using Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
