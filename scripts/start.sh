#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f .env ]] || { echo "Run ./scripts/setup.sh first." >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "Docker Engine is required." >&2; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "Docker Compose plugin is required." >&2; exit 1; }
password="$(sed -n 's/^POSTGRES_PASSWORD=//p' .env | tail -n1)"
[[ -n "$password" && "$password" != "replace-with-a-long-random-password" ]] || {
  echo "Set a unique POSTGRES_PASSWORD in .env." >&2
  exit 1
}
docker compose up --build -d
port="$(sed -n 's/^AURORA_HTTP_PORT=//p' .env | tail -n1)"
port="${port:-8088}"
echo "AURORA containers started. Checking dashboard health..."
for attempt in $(seq 1 60); do
  if curl --fail --silent "http://127.0.0.1:$port/" >/dev/null; then
    echo "Dashboard is responding at http://localhost:$port"
    exit 0
  fi
  sleep 2
done
docker compose ps
echo "Dashboard did not become ready within 120 seconds. Inspect: docker compose logs --tail=150" >&2
exit 1
