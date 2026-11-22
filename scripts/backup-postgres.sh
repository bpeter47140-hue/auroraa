#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f .env ]]; then echo "Missing .env. Copy .env.example and configure it first." >&2; exit 1; fi
set -a
# shellcheck disable=SC1091
source .env
set +a
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
mkdir -p backups
chmod 700 backups
umask 077
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="backups/aurora-postgres-${stamp}.dump"
docker compose exec -T database pg_dump -U aurora -d aurora -Fc > "$file"
test -s "$file"
sha256sum "$file" > "${file}.sha256"
echo "Backup created: $file"
echo "Keep this file private: it contains application and customer data."
