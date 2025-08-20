# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Leverage caching
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Only copy the fat jar (adjust name/glob if needed)
COPY --from=build /app/target/*SNAPSHOT.jar app.jar

# Keep memory in check on small instances
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

# Bind to platform-provided PORT
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080} --server.address=0.0.0.0"]