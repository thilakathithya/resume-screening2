# ---------- STAGE 1: Build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- STAGE 2: Run ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Directory for the H2 file database (persisted via volume)
RUN mkdir -p /data && chown appuser:appgroup /data

COPY --from=build /app/target/resume-screening-agent.jar app.jar

ENV DB_PATH=/data/resumedb
ENV SERVER_PORT=8080

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
