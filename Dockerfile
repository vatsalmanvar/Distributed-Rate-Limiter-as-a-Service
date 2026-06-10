FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S ratelimiter && adduser -S ratelimiter -G ratelimiter
WORKDIR /app
COPY --from=build /workspace/target/rate-limiter-service-0.1.0-SNAPSHOT.jar /app/rate-limiter-service.jar
USER ratelimiter
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/rate-limiter-service.jar"]
