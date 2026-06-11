#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "1/6 Checking prerequisites..."
command -v git >/dev/null 2>&1 || { echo "git not found"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "docker not found"; exit 1; }
if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose not found or not available as 'docker compose'. Try 'docker-compose' if installed."
fi

echo "2/6 Ensure you're in the repo root: $ROOT_DIR"

echo "3/6 Preparing environment file..."
if [ -f .env.example ] && [ ! -f .env ]; then
  cp .env.example .env
  echo "Copied .env.example -> .env (please edit .env if needed)"
else
  echo ".env already exists or .env.example missing; verify .env values"
fi

echo "4/6 Starting infrastructure with Docker Compose..."
# choose docker compose command
if docker compose version >/dev/null 2>&1; then
  DC_CMD="docker compose"
else
  DC_CMD="docker-compose"
fi

# start infra
eval "$DC_CMD up -d"

# helper: wait for a service to become healthy and print diagnostics on failure
wait_for_healthy() {
  svc="$1"
  timeout="${2:-120}"
  interval=5
  elapsed=0
  echo "Waiting up to ${timeout}s for ${svc} to become healthy..."
  while [ "$elapsed" -lt "$timeout" ]; do
    cid=$(eval "$DC_CMD ps -q $svc" 2>/dev/null || true)
    if [ -n "$cid" ]; then
      health=$(docker inspect --format '{{json .State.Health.Status}}' "$cid" 2>/dev/null || true)
      if [ "$health" = "\"healthy\"" ] || [ "$health" = "healthy" ]; then
        echo "${svc} is healthy"
        return 0
      fi
    fi
    sleep $interval
    elapsed=$((elapsed + interval))
  done

  echo "${svc} did not become healthy within ${timeout}s"
  if [ -n "$cid" ]; then
    echo "Container ID: $cid"
    echo "---- Container state ----"
    docker inspect --format '{{json .State}}' "$cid" || true
    echo "---- Health detail ----"
    docker inspect --format '{{json .State.Health}}' "$cid" || true
    echo "---- Last 500 lines of logs ----"
    eval "$DC_CMD logs --no-color $svc --tail 500" || eval "$DC_CMD logs $svc --tail 500" || true
    echo "---- docker stats (instant) ----"
    docker stats --no-stream "$cid" || true
  else
    echo "No container id for $svc yet; listing compose services:"
    eval "$DC_CMD ps -a | sed -n '1,200p' || true"
  fi

  echo "Attempting one force-recreate of $svc to see if it recovers..."
  eval "$DC_CMD up -d --force-recreate $svc" || true
  sleep 5
  cid2=$(eval "$DC_CMD ps -q $svc" 2>/dev/null || true)
  if [ -n "$cid2" ]; then
    echo "Post-recreate container id: $cid2"
    docker inspect --format '{{json .State.Health.Status}}' "$cid2" 2>/dev/null || true
    eval "$DC_CMD logs --no-color $svc --tail 200" || true
  fi

  return 2
}

# Wait for zookeeper service to become healthy (adjust service name if different)
if ! wait_for_healthy rate-limiter-zookeeper 120; then
  echo "Zookeeper failed to become healthy. Check the logs: $ROOT_DIR/zookeeper-logs.txt or run:"
  echo "  docker compose logs rate-limiter-zookeeper --tail 500 > zookeeper-logs.txt"
  exit 1
fi

echo "5/6 Installing dependencies (detecting project type)..."
if [ -f package.json ]; then
  if command -v pnpm >/dev/null 2>&1; then
    pnpm install
  elif command -v npm >/dev/null 2>&1; then
    npm install
  else
    echo "npm or pnpm not found; install Node.js"
    exit 1
  fi
fi

if [ -f go.mod ]; then
  command -v go >/dev/null 2>&1 || { echo "go not found"; exit 1; }
  go mod download
fi

echo "6/6 Build or start services (manual step may be required). Common commands:"
echo " - npm run build && npm run start"
echo " - npm run dev"
echo " - go build -o bin/service ./cmd/service && ./bin/service"
echo " - docker compose up --build -d <service-name>"
echo ""
echo "Now run migrations and tests as required (see SETUP.md)."
