# ==========================================
# Stage 1: Build & Package with Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application
COPY src ./src
RUN mvn clean package -DskipTests=false

# ==========================================
# Stage 2: Minimal Production JRE Image
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root service user for container security
RUN addgroup -S retailgroup && adduser -S retailuser -G retailgroup

# Copy compiled JAR from builder stage
COPY --from=builder /build/target/online-retail-app-1.0.0.jar app.jar
RUN chown -R retailuser:retailgroup /app

USER retailuser

# Expose HTTP port (8080) for web app and Actuator metrics
EXPOSE 8080

# Configure JVM memory limits for container environments
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Healthcheck probe using Actuator endpoint
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
