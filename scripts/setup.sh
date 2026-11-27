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
runner_token="$(openssl rand -hex 32)"
preview_signing_secret="$(openssl rand -hex 32)"
browser_qa_token="$(openssl rand -hex 32)"
docker_gid="$(stat -c %g /var/run/docker.sock 2>/dev/null || getent group docker | cut -d: -f3 || echo 947)"
python3 - "$password" "$runner_token" "$preview_signing_secret" "$browser_qa_token" "$docker_gid" <<'PY'
from pathlib import Path
import sys
path = Path(".env")
text = path.read_text()
replacements = {
    "POSTGRES_PASSWORD=replace-with-a-long-random-password": "POSTGRES_PASSWORD=" + sys.argv[1],
    "AURORA_PREVIEW_RUNNER_TOKEN=replace-with-a-unique-random-secret": "AURORA_PREVIEW_RUNNER_TOKEN=" + sys.argv[2],
    "AURORA_PREVIEW_SIGNING_SECRET=replace-with-another-unique-random-secret": "AURORA_PREVIEW_SIGNING_SECRET=" + sys.argv[3],
    "AURORA_BROWSER_QA_TOKEN=replace-with-a-unique-browser-qa-secret": "AURORA_BROWSER_QA_TOKEN=" + sys.argv[4],
    "DOCKER_GID=947": "DOCKER_GID=" + sys.argv[5],
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"Missing setup placeholder: {old.split('=', 1)[0]}")
    text = text.replace(old, new)
path.write_text(text)
PY
chmod 600 .env
echo "Created .env with unique 256-bit database and service secrets (permissions 600)."
echo "AI provider is disabled until you add AURORA_AI_API_KEY."
echo "First-owner registration is disabled by default."
echo "Before first sign-up, temporarily set AURORA_BOOTSTRAP_ENABLED=true in .env."
echo "Immediately after creating the owner, set it back to false."
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed. Install Docker Engine and the Compose plugin before starting AURORA."
else
  docker compose version
fi
