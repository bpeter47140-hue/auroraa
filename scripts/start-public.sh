#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

[[ -f .env ]] || { echo "Missing .env. Run ./scripts/setup.sh first." >&2; exit 1; }
chmod 600 .env

domain="$(python3 - <<'PY'
from pathlib import Path
from urllib.parse import urlsplit
import re
values = {}
for raw in Path(".env").read_text().splitlines():
    line = raw.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    key, value = line.split("=", 1)
    values[key.strip()] = value.strip().strip("'").strip('"')

domain = values.get("AURORA_DOMAIN", "")
base = values.get("AURORA_APP_BASE_URL", "")
password = values.get("POSTGRES_PASSWORD", "")
bootstrap = values.get("AURORA_BOOTSTRAP_ENABLED", "false").lower()
if not domain or domain != domain.lower() or domain == "aurora.example.com" or domain.endswith((".localhost", ".local", ".test", ".invalid", ".example")) or not re.fullmatch(
    r"(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}", domain
):
    raise SystemExit("Set AURORA_DOMAIN to your real fully-qualified public hostname in .env.")
parsed = urlsplit(base)
if parsed.scheme != "https" or parsed.hostname != domain or parsed.port is not None or parsed.username or parsed.password or parsed.path not in ("", "/") or parsed.query or parsed.fragment:
    raise SystemExit("Set AURORA_APP_BASE_URL to exactly https://<AURORA_DOMAIN> in .env.")
if len(password) < 32 or password == "replace-with-a-long-random-password":
    raise SystemExit("Set a unique POSTGRES_PASSWORD in .env (at least 32 characters).")
if bootstrap != "false":
    raise SystemExit("Refusing public startup while AURORA_BOOTSTRAP_ENABLED is not false.")
print(domain)
PY
)"

echo "Public deployment configuration checks passed for $domain."
command -v docker >/dev/null 2>&1 || { echo "Docker Engine is required." >&2; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "Docker Compose plugin is required." >&2; exit 1; }

AURORA_DOMAIN="$domain" docker compose --profile public config --quiet
AURORA_DOMAIN="$domain" docker compose --profile public up --build -d

echo "Waiting for the HTTPS dashboard at https://$domain/ ..."
for attempt in $(seq 1 60); do
  if curl --fail --silent --show-error --max-time 10 "https://$domain/" >/dev/null 2>&1; then
    echo "AURORA is responding over HTTPS at https://$domain/"
    exit 0
  fi
  sleep 5
done

AURORA_DOMAIN="$domain" docker compose --profile public ps
echo "HTTPS did not become ready within 300 seconds. Inspect Caddy and dashboard logs." >&2
AURORA_DOMAIN="$domain" docker compose --profile public logs --tail=100 caddy dashboard >&2
exit 1
