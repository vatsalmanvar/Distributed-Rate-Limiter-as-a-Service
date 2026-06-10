# Distributed Rate Limiter as a Service

A production-oriented, multi-tenant distributed rate limiter built with Java 17, Spring Boot 3, Redis, PostgreSQL, Kafka, and Docker Compose.

## Implemented Contents

The service now includes the backend foundation plus Phase 2 algorithms and Phase 3 APIs:

- Maven build with Spring Boot, Redis, PostgreSQL, Kafka, validation, security, actuator, and test dependencies.
- Docker Compose topology for the application, Redis with append-only persistence, PostgreSQL with initialization SQL, Kafka, ZooKeeper, and Kafka UI.
- Spring configuration for Redis/Lettuce pooling, Kafka producers and consumers, stateless security defaults, JPA, and service properties.
- JPA entities and repositories for tenants and rate limit rules.
- Immutable Kafka event payload model for rate limit decisions.
- Redis Lua-backed fixed window, sliding-window log, sliding-window counter, and token bucket algorithms.
- Tenant and rate limit rule administration APIs.
- Main rate limit check API with asynchronous Kafka event publication.
- Kafka analytics consumer with persisted event history and summary APIs.

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

## API Overview

- `POST /api/v1/tenants` creates a tenant and returns a one-time API key.
- `POST /api/v1/tenants/{tenantId}/rules` creates rate limit rules for `GLOBAL`, `PER_IP`, or `PER_USER` scopes.
- `POST /api/v1/rate-limit/check` evaluates a request using the `X-API-Key` header.
- `GET /api/v1/analytics/tenants/{tenantId}/events` returns the latest persisted events.
- `GET /api/v1/analytics/tenants/{tenantId}/summary` returns aggregate allow/reject counts.
