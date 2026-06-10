# Distributed Rate Limiter as a Service

A production-oriented, multi-tenant distributed rate limiter built with Java 17, Spring Boot 3, Redis, PostgreSQL, Kafka, Docker Compose, and a React operations dashboard.

## What is included

### Backend

- Spring Boot REST API for tenant administration, rule management, and rate-limit decisions.
- Redis-backed distributed algorithms:
  - Token bucket
  - Fixed window
  - Sliding-window log
  - Sliding-window counter
- PostgreSQL persistence for tenants, rules, and analytics events.
- Kafka event publication for every rate-limit decision and a consumer that persists analytics.
- Live WebSocket stream at `/ws/analytics` for dashboard updates.
- Global JSON exception handler for validation, typed request errors, API errors, and unexpected failures.
- Stateless Spring Security baseline and permissive CORS for local dashboard development.

### Dashboard

The `frontend/` directory contains a Vite React app configured with Tailwind CSS, Axios, and Recharts.

- **Dashboard page**: live WebSocket event table, summary cards, decision pie chart, and a quick rate-limit check form.
- **Rules page**: tenant creation, one-time API key display, rule creation, and rule deletion.
- **Analytics page**: historical summary metrics, hourly traffic chart, algorithm usage chart, and recent events table.

## Architecture diagram description

```text
Client / SDK / Demo Form
        |
        | POST /api/v1/rate-limit/check with X-API-Key
        v
Spring Boot Rate Limiter API
        |
        | 1. Resolve tenant by hashed API key from PostgreSQL
        | 2. Resolve endpoint-specific or tenant-wide rule
        | 3. Execute selected Lua-backed algorithm in Redis
        | 4. Return allow/reject decision
        |
        +--> Kafka topic: rate-limit-events
        |         |
        |         v
        |   Analytics consumer persists events to PostgreSQL
        |
        +--> WebSocket broadcaster: /ws/analytics
                  |
                  v
            React dashboard live updates
```

Supporting infrastructure in Docker Compose:

- **Redis** stores counters, token buckets, and sliding-window data structures.
- **PostgreSQL** stores tenants, rules, and analytics history.
- **Kafka/ZooKeeper** decouples decision traffic from analytics persistence.
- **Kafka UI** provides local event inspection.
- **React dashboard** runs as a Vite service and talks to the backend over REST and WebSockets.

## Local development

### Start the complete stack

```bash
docker compose up --build
```

Services:

| Service | Port | URL |
| --- | ---: | --- |
| React dashboard | 5173 | http://localhost:5173 |
| Spring Boot API | 8080 | http://localhost:8080 |
| Redis | 6379 | localhost:6379 |
| PostgreSQL | 5432 | localhost:5432 |
| Kafka | 9092 | localhost:9092 |
| ZooKeeper | 2181 | localhost:2181 |
| Kafka UI | 8090 | http://localhost:8090 |

### Run backend only

```bash
mvn spring-boot:run
```

The backend expects local PostgreSQL, Redis, and Kafka unless you override the environment variables listed below.

### Run dashboard only

```bash
cd frontend
npm install
npm run dev
```

By default, Vite proxies `/api` and `/ws` to `http://localhost:8080`.

## Configuration

Backend configuration is defined in `src/main/resources/application.yml` and can be overridden with environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Spring Boot HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/ratelimiter` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `ratelimiter` | PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | `ratelimiter` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |

Dashboard configuration:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api/v1` | REST API base URL |
| `VITE_WS_URL` | same host `/ws/analytics` | WebSocket endpoint |

## API documentation

### Create tenant

`POST /api/v1/tenants`

Request:

```json
{
  "name": "Acme API",
  "plan": "PRO"
}
```

Response includes the tenant and a one-time plaintext API key. Store it immediately; only a SHA-256 hash is persisted.

```json
{
  "tenant": {
    "id": "9c95b6d4-5ca7-4a75-9d29-94c64f1e4a1e",
    "name": "Acme API",
    "plan": "PRO",
    "createdAt": "2026-06-10T16:00:00Z"
  },
  "apiKey": "rl_example"
}
```

### List tenants

`GET /api/v1/tenants`

Returns all tenants without API keys.

### Get tenant

`GET /api/v1/tenants/{tenantId}`

Returns a single tenant.

### Delete tenant

`DELETE /api/v1/tenants/{tenantId}`

Deletes the tenant and its rules.

### Create rule

`POST /api/v1/tenants/{tenantId}/rules`

Request:

```json
{
  "scope": "PER_IP",
  "algorithm": "TOKEN_BUCKET",
  "limit": 100,
  "windowSizeMs": 60000,
  "endpoint": "/api/orders",
  "failStrategy": "FAIL_OPEN"
}
```

Fields:

| Field | Values | Notes |
| --- | --- | --- |
| `scope` | `PER_IP`, `PER_USER`, `GLOBAL` | `GLOBAL` ignores request identifier |
| `algorithm` | `TOKEN_BUCKET`, `FIXED_WINDOW`, `SLIDING_WINDOW_LOG`, `SLIDING_WINDOW_COUNTER` | Redis-backed strategy |
| `limit` | positive integer | Maximum requests in window |
| `windowSizeMs` | positive integer | Window size in milliseconds |
| `endpoint` | string or `null` | Blank/null rule applies to all endpoints for the scope |
| `failStrategy` | `FAIL_OPEN`, `FAIL_CLOSE` | Behavior if Redis evaluation fails |

### List rules

`GET /api/v1/tenants/{tenantId}/rules`

Returns all rules for a tenant.

### Delete rule

`DELETE /api/v1/tenants/{tenantId}/rules/{ruleId}`

Deletes one rule owned by the tenant.

### Check rate limit

`POST /api/v1/rate-limit/check`

Headers:

```http
X-API-Key: rl_your_key
```

Request:

```json
{
  "scope": "PER_IP",
  "identifier": "203.0.113.10",
  "endpoint": "/api/orders"
}
```

Response:

```json
{
  "allowed": true,
  "remaining": 99,
  "retryAfterMs": 0,
  "resetAfterMs": 60000,
  "tenantId": "9c95b6d4-5ca7-4a75-9d29-94c64f1e4a1e",
  "ruleId": "4a3f26b8-d3af-43df-9a2d-04f0f8f2daca",
  "algorithm": "TOKEN_BUCKET"
}
```

### Recent analytics events

`GET /api/v1/analytics/tenants/{tenantId}/events`

Returns the latest 100 persisted rate-limit decision events.

### Analytics summary

`GET /api/v1/analytics/tenants/{tenantId}/summary?since=2026-06-10T00:00:00Z`

Returns total, allowed, and rejected counts since the provided timestamp. If `since` is omitted, the backend uses the last 24 hours.

### Live analytics WebSocket

`ws://localhost:8080/ws/analytics`

Optional tenant filter:

`ws://localhost:8080/ws/analytics?tenantId={tenantId}`

Each message is a JSON `RateLimitEvent`:

```json
{
  "tenantId": "9c95b6d4-5ca7-4a75-9d29-94c64f1e4a1e",
  "identifier": "203.0.113.10",
  "endpoint": "/api/orders",
  "allowed": true,
  "algorithm": "TOKEN_BUCKET",
  "timestamp": 1781107200000,
  "remainingTokens": 99
}
```

## Example cURL flow

```bash
TENANT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/tenants \
  -H 'Content-Type: application/json' \
  -d '{"name":"Acme API","plan":"PRO"}')

TENANT_ID=$(echo "$TENANT_RESPONSE" | jq -r '.tenant.id')
API_KEY=$(echo "$TENANT_RESPONSE" | jq -r '.apiKey')

curl -X POST "http://localhost:8080/api/v1/tenants/$TENANT_ID/rules" \
  -H 'Content-Type: application/json' \
  -d '{"scope":"PER_IP","algorithm":"TOKEN_BUCKET","limit":5,"windowSizeMs":60000,"endpoint":"/api/orders","failStrategy":"FAIL_OPEN"}'

curl -X POST http://localhost:8080/api/v1/rate-limit/check \
  -H "X-API-Key: $API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"scope":"PER_IP","identifier":"203.0.113.10","endpoint":"/api/orders"}'
```

## Notes

- Unit tests are intentionally not included per project request.
- `spring.jpa.hibernate.ddl-auto` is currently `create` for local/demo startup. Use migrations and `validate` or `none` for production deployments.
- The dashboard stores the copied API key in browser local storage for local convenience.
