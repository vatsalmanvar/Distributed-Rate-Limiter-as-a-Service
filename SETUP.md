# Local Setup

1) Prerequisites — install if missing
- Git: https://git-scm.com/
- Docker & Docker Compose: https://docs.docker.com/
- Node.js (>=16) and npm or pnpm if project uses Node
- Go if the project uses Go

Quick checks:
- git --version
- docker --version
- docker compose version
- node --version (if applicable)
- go version (if applicable)

2) Clone or update repository
- If not cloned:
  git clone <REPO_URL> /Users/vatsal/Developer/Distributed-rate-limiting-as-a-service/Distributed-Rate-Limiter-as-a-Service
  cd /Users/vatsal/Developer/Distributed-rate-limiting-as-a-service/Distributed-Rate-Limiter-as-a-Service
- If already cloned:
  cd /Users/vatsal/Developer/Distributed-rate-limiting-as-a-service/Distributed-Rate-Limiter-as-a-Service
  git fetch origin
  git checkout main
  git pull

3) Environment
- Copy example env and edit:
  cp .env.example .env
  # Edit .env (API keys, DB URLs, ports...)

4) Start infra (Redis, Postgres, etc.)
- Prefer Docker Compose if provided:
  docker compose up -d
  # or
  docker-compose up -d

5) Install dependencies
- Node.js project:
  npm install
  # or
  pnpm install
  # or
  yarn install
- Go project:
  go mod download

6) Database migrations (if applicable)
- Example:
  npm run migrate
  # or
  ./bin/migrate up
  # or
  go run ./cmd/migrate

7) Build & run
- Node:
  npm run build
  npm run start
  # or for development:
  npm run dev
- Go:
  go build -o bin/service ./cmd/service
  ./bin/service
- Or use Docker Compose service:
  docker compose up --build -d <service-name>

8) Run tests
  npm test
  # or
  go test ./...

9) Stop / cleanup
  docker compose down
  docker compose down -v  # to remove volumes

If any command above doesn't match your repo layout, replace with the equivalent script names from package.json or project README.

## Troubleshooting: zookeeper container is "unhealthy"

If docker-compose reports the zookeeper container as unhealthy, run these exact commands now and paste the generated files or outputs so I can give a precise fix.

1) Quick status & logs (save to files)
- docker compose ps > compose-ps.txt
- docker compose logs rate-limiter-zookeeper --tail 1000 > zookeeper-logs.txt

2) Container inspect & healthcheck details
- cid=$(docker compose ps -q rate-limiter-zookeeper)
- echo "$cid" > zookeeper-cid.txt
- docker inspect "$cid" > zookeeper-inspect.json
- docker inspect --format '{{json .State.Health}}' "$cid" > zookeeper-health.json || true
- docker inspect --format '{{json .Config.Healthcheck}}' "$cid" > zookeeper-healthcheck.json || true

3) Check host port conflicts (ZK default client port 2181)
- sudo lsof -iTCP -sTCP:LISTEN -P -n | grep 2181 || true
- nc -vz 127.0.0.1 2181 || true

4) Common quick fixes
- Port conflict: stop the conflicting process or change ports in docker-compose.yml.
- Docker resource limits (Mac/Windows): increase Docker Desktop RAM/CPU if you see JVM/OOM errors in logs.
- Corrupted data: stop zookeeper, remove its volume (data loss), then recreate:
  docker compose stop rate-limiter-zookeeper
  docker compose rm -f rate-limiter-zookeeper
  docker compose down -v   # removes volumes, if acceptable
  docker compose up -d rate-limiter-zookeeper
- Healthcheck issues: if the healthcheck test uses nc (netcat) but the image lacks it, either install nc in the image or increase the healthcheck start_period/retries in docker-compose.yml. Example:
```yaml
healthcheck:
  test: ["CMD-SHELL", "echo ruok | nc -w 3 localhost 2181 | grep imok"]
  interval: 10s
  timeout: 5s
  retries: 12
  start_period: 30s
```

5) What to paste here for focused help
- zookeeper-logs.txt
- zookeeper-inspect.json
- docker-compose.yml (zookeeper service block or entire file)

If you paste those three items I will analyze the logs and recommend the exact docker-compose.yml change or data-action to fix zookeeper.
