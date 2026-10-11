# Multi-stage Dockerfile for Spring Boot on Render
# Stage 1: Build the JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies first (leveraging Docker layer cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create audio temp directory if needed
RUN mkdir -p /tmp/audio

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Render injects PORT dynamically (default 8080 if not supplied)
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
