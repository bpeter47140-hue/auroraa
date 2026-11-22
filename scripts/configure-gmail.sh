#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

read -rp "Gmail address used to send AURORA invitations: " mail_address
if [[ ! "$mail_address" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]]; then
  echo "That email address doesn't look valid." >&2
  exit 1
fi

echo "Paste the 16-character Google App Password (spaces are okay)."
read -rsp "App Password: " app_password
echo
app_password="${app_password// /}"
if [[ ! "$app_password" =~ ^[A-Za-z0-9]{16}$ ]]; then
  echo "Expected a 16-character Google App Password. Nothing was saved." >&2
  unset app_password
  exit 1
fi

read -rp "Public HTTPS AURORA dashboard URL (required): " app_url
if [[ -z "$app_url" ]]; then
  echo "A public HTTPS dashboard URL is required. Nothing was saved." >&2
  exit 1
fi
if ! python3 - "$app_url" <<'PYURL'
import sys
from urllib.parse import urlparse
u = urlparse(sys.argv[1])
if (u.scheme != "https" or not u.hostname or u.hostname.lower() in {"localhost", "127.0.0.1", "::1"}
        or u.username or u.password or u.query or u.fragment):
    raise SystemExit(1)
PYURL
then
  echo "Use a public HTTPS URL without credentials, query parameters or fragments. Nothing was saved." >&2
  exit 1
fi

export AURORA_SETUP_MAIL_ADDRESS="$mail_address"
export AURORA_SETUP_MAIL_PASSWORD="$app_password"
export AURORA_SETUP_APP_URL="$app_url"
python3 <<'PY'
import os
from pathlib import Path

path = Path(".env")
lines = path.read_text().splitlines() if path.exists() else []
updates = {
    "AURORA_MAIL_ENABLED": "true",
    "AURORA_MAIL_FROM": os.environ["AURORA_SETUP_MAIL_ADDRESS"],
    "AURORA_MAIL_USERNAME": os.environ["AURORA_SETUP_MAIL_ADDRESS"],
    "AURORA_MAIL_PASSWORD": os.environ["AURORA_SETUP_MAIL_PASSWORD"],
    "AURORA_APP_BASE_URL": os.environ["AURORA_SETUP_APP_URL"],
}
remove = {
    "AURORA_GMAIL_OAUTH_CLIENT_ID",
    "AURORA_GMAIL_OAUTH_CLIENT_SECRET",
    "AURORA_GMAIL_OAUTH_REFRESH_TOKEN",
}
result = []
seen = set()
for line in lines:
    key = line.split("=", 1)[0] if "=" in line else ""
    if key in remove:
        continue
    if key in updates:
        if key not in seen:
            result.append(f"{key}={updates[key]}")
            seen.add(key)
    else:
        result.append(line)
for key, value in updates.items():
    if key not in seen:
        result.append(f"{key}={value}")
path.write_text("\n".join(result).rstrip() + "\n")
path.chmod(0o600)
PY
unset AURORA_SETUP_MAIL_PASSWORD app_password
echo "Gmail SMTP settings saved in .env (permissions: 600)."
echo "Restart AURORA's backend to load the settings."
