FROM openjdk:21-jdk
COPY build/libs/MedicalRecordSystem-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]