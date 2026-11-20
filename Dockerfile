# Build with the same JDK the project targets (Java 21).
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /home/app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Runtime: Java 21 JRE (GA release, not an early-access JDK), unprivileged user.
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /home/app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
