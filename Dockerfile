FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
# Create a system group and user in Debian-based image
RUN groupadd -r ratelimiter && useradd -r -g ratelimiter -s /usr/sbin/nologin -d /nonexistent -c "ratelimiter user" ratelimiter || true
WORKDIR /app
COPY --from=build /workspace/target/rate-limiter-service-0.1.0-SNAPSHOT.jar /app/rate-limiter-service.jar
USER ratelimiter
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/rate-limiter-service.jar"]
