#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f .env ]]; then echo "Missing .env. Configure the database first." >&2; exit 1; fi
set -a
# shellcheck disable=SC1091
source .env
set +a
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
backup="${1:-}"
if [[ -z "$backup" || ! -f "$backup" ]]; then echo "Usage: $0 backups/aurora-postgres-YYYYMMDDTHHMMSSZ.dump" >&2; exit 1; fi
if [[ ! -f "${backup}.sha256" ]]; then echo "Missing checksum sidecar: ${backup}.sha256. Refusing an unverified restore." >&2; exit 1; fi
(cd "$(dirname "$backup")" && sha256sum -c "$(basename "$backup").sha256")
# Validate the archive before prompting for destructive replacement.
docker compose exec -T database pg_restore --list < "$backup" > /dev/null
printf 'WARNING: this replaces objects/data in the configured Aurora database.\nType RESTORE to continue: '
read -r confirmation
if [[ "$confirmation" != "RESTORE" ]]; then echo "Restore cancelled."; exit 1; fi
docker compose exec -T database pg_restore -U aurora -d aurora --clean --if-exists --no-owner --exit-on-error --single-transaction < "$backup"
echo "Restore command completed. Verify application health and data before reopening access."
