#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -e .env ]]; then
  echo ".env already exists; leaving it untouched."
  exit 0
fi
if [[ ! -f .env.example ]]; then
  echo "Missing .env.example." >&2
  exit 1
fi
cp .env.example .env
password="$(openssl rand -hex 32)"
python3 - "$password" <<'PY'
from pathlib import Path
import sys
path = Path(".env")
text = path.read_text()
text = text.replace("POSTGRES_PASSWORD=replace-with-a-long-random-password",
                    "POSTGRES_PASSWORD=" + sys.argv[1])
path.write_text(text)
PY
chmod 600 .env
echo "Created .env with a unique 256-bit database password (permissions 600)."
echo "AI provider is disabled until you add AURORA_AI_API_KEY."
echo "First-owner registration is disabled by default."
echo "Before first sign-up, temporarily set AURORA_BOOTSTRAP_ENABLED=true in .env."
echo "Immediately after creating the owner, set it back to false."
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed. Install Docker Engine and the Compose plugin before starting AURORA."
else
  docker compose version
fi
