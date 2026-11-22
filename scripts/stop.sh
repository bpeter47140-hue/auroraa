#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f .env ]] || { echo "No .env found." >&2; exit 1; }
docker compose down
echo "AURORA stopped. Persistent PostgreSQL volume was retained."
