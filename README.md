# Distributed Rate Limiter as a Service

A production-oriented, multi-tenant distributed rate limiter built with Java 17, Spring Boot 3, Redis, PostgreSQL, Kafka, and Docker Compose.

## Phase 1 Contents

This initial phase establishes the backend foundation:

- Maven build with Spring Boot, Redis, PostgreSQL, Kafka, validation, security, actuator, and test dependencies.
- Docker Compose topology for the application, Redis with append-only persistence, PostgreSQL with initialization SQL, Kafka, ZooKeeper, and Kafka UI.
- Spring configuration for Redis/Lettuce pooling, Kafka producers and consumers, stateless security defaults, JPA, and service properties.
- JPA entities and repositories for tenants and rate limit rules.
- Immutable Kafka event payload model for rate limit decisions.

## Local Development

```bash
docker compose up --build
```

Services:

| Service | Port |
| --- | --- |
| Spring Boot API | 8080 |
| Redis | 6379 |
| PostgreSQL | 5432 |
| Kafka | 9092 |
| ZooKeeper | 2181 |
| Kafka UI | 8090 |

## Configuration

Runtime configuration is provided by `src/main/resources/application.yml` and environment variables in `docker-compose.yml`.
