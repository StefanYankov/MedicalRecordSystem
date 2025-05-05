# Stage 1: Build the application
FROM gradle:8.10-jdk21 AS builder

# Set working directory for build
WORKDIR /app

# Copy Gradle configuration and source code
COPY build.gradle settings.gradle ./
COPY src ./src

# Build the application, skipping tests to speed up
RUN gradle clean build -x test

# Debug: List build output to verify JAR
RUN ls -l /app/build/libs/

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre

# Set working directory for runtime
WORKDIR /app

# Copy the Spring Boot JAR from the builder stage
COPY --from=builder /app/build/libs/MedicalRecordSystem-0.0.1-SNAPSHOT.jar app.jar

# Expose the Spring Boot port
EXPOSE 8080

# Add healthcheck for monitoring
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with the dev profile
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}", "-jar", "app.jar"]