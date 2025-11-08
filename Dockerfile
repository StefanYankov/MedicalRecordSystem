# Use a valid base image with Java 21
FROM eclipse-temurin:21-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the Gradle wrapper files
COPY gradlew .
COPY gradle ./gradle

# Copy the build configuration files
COPY build.gradle .
COPY settings.gradle .

# Grant executable permissions to the Gradle wrapper
RUN chmod +x ./gradlew

# Download dependencies to leverage Docker layer caching
RUN ./gradlew dependencies --no-daemon

# Copy the wait-for-it script and make it executable
COPY wait-for-it.sh .
RUN chmod +x ./wait-for-it.sh

# Copy the rest of the application source code
COPY src ./src

# Build the application, skipping tests
RUN ./gradlew bootJar --no-daemon -x test

# Expose the port the Spring Boot application runs on
EXPOSE 8080

# Use the wait-for-it.sh to wait for Keycloak before starting the application.
ENTRYPOINT ["./wait-for-it.sh", "keycloak:8080", "java", "-jar", "build/libs/MedicalRecordSystem-0.0.1-SNAPSHOT.jar"]
