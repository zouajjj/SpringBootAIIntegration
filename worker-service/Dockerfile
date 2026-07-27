# --- Stage 1: build worker-service ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY worker-service/pom.xml ./
RUN mvn -B dependency:go-offline
COPY worker-service/src ./src
RUN mvn -B clean package -DskipTests

# --- Stage 2: slim runtime image ---
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
