#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f .env ]]; then bash ./scripts/setup.sh; fi

python3 <<'PY'
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from urllib.parse import urlencode, urlparse, parse_qs
from urllib.request import Request, urlopen
import json, secrets, webbrowser

sender = input("Gmail address AURORA will send invitations from: ").strip()
if "@" not in sender or any(ch.isspace() for ch in sender):
    raise SystemExit("Enter a valid email address.")
client_json_path = Path(input("Path to the downloaded Google OAuth Desktop App JSON file: ").strip()).expanduser()
if not client_json_path.is_file():
    raise SystemExit("That OAuth JSON file was not found.")
try:
    oauth_config = json.loads(client_json_path.read_text()).get("installed", {})
    client_id = oauth_config["client_id"]
    client_secret = oauth_config["client_secret"]
except (ValueError, KeyError, TypeError):
    raise SystemExit("Choose the JSON file downloaded for an OAuth client of type Desktop app.")
base_url = input("Dashboard URL for invitation links [http://localhost:5173]: ").strip() or "http://localhost:5173"
if not base_url.startswith(("http://", "https://")) or any(ch.isspace() for ch in base_url):
    raise SystemExit("Enter a valid http:// or https:// URL.")

redirect_uri = "http://127.0.0.1:8765/"
state = secrets.token_urlsafe(24)
result = {}

class Callback(BaseHTTPRequestHandler):
    def do_GET(self):
        query = parse_qs(urlparse(self.path).query)
        if query.get("state", [""])[0] != state:
            self.send_response(400); self.end_headers()
            self.wfile.write(b"OAuth state did not match. You can close this tab.")
            result["error"] = "OAuth state mismatch"
            return
        if "error" in query:
            result["error"] = query["error"][0]
        else:
            result["code"] = query.get("code", [""])[0]
        body = b"AURORA Gmail authorization received. You can close this browser tab and return to the terminal."
        self.send_response(200); self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body)
    def log_message(self, *_):
        pass

params = {
    "client_id": client_id,
    "redirect_uri": redirect_uri,
    "response_type": "code",
    "scope": "https://www.googleapis.com/auth/gmail.send",
    "access_type": "offline",
    "prompt": "consent",
    "state": state,
}
auth_url = "https://accounts.google.com/o/oauth2/v2/auth?" + urlencode(params)
server = HTTPServer(("127.0.0.1", 8765), Callback)
server.timeout = 180
print("\nOpening Google authorization. Sign in to the sending account and approve Gmail sending access.")
if not webbrowser.open(auth_url):
    print("If your browser did not open, copy this URL into it:\n" + auth_url)
server.handle_request()
server.server_close()
if result.get("error") or not result.get("code"):
    raise SystemExit("Google authorization was not completed: " + result.get("error", "no callback received"))

form = urlencode({
    "code": result["code"],
    "client_id": client_id,
    "client_secret": client_secret,
    "redirect_uri": redirect_uri,
    "grant_type": "authorization_code",
}).encode()
request = Request("https://oauth2.googleapis.com/token", data=form, headers={"Content-Type": "application/x-www-form-urlencoded"})
try:
    with urlopen(request, timeout=20) as response:
        tokens = json.loads(response.read())
except Exception as exc:
    raise SystemExit("Google token exchange failed: " + type(exc).__name__)
refresh_token = tokens.get("refresh_token")
if not refresh_token:
    raise SystemExit("Google did not return a refresh token. Revoke AURORA access in your Google Account and run this script again.")

path = Path(".env")
lines = path.read_text().splitlines() if path.exists() else []
values = {
    "AURORA_MAIL_ENABLED": "true",
    "AURORA_MAIL_FROM": sender,
    "AURORA_GMAIL_OAUTH_CLIENT_ID": client_id,
    "AURORA_GMAIL_OAUTH_CLIENT_SECRET": client_secret,
    "AURORA_GMAIL_OAUTH_REFRESH_TOKEN": refresh_token,
    "AURORA_APP_BASE_URL": base_url.rstrip("/"),
}
old_keys = {
    "AURORA_MAIL_HOST", "AURORA_MAIL_PORT", "AURORA_MAIL_USERNAME",
    "AURORA_MAIL_PASSWORD", "AURORA_MAIL_SMTP_AUTH", "AURORA_MAIL_STARTTLS"
}
lines = [line for line in lines if line.partition("=")[0] not in old_keys and line.partition("=")[0] not in values]
lines.extend(f"{key}={value}" for key, value in values.items())
path.write_text("\n".join(lines) + "\n")
path.chmod(0o600)
print("\nGmail OAuth credentials saved to .env (permissions 600). Secrets were not printed.")
print("Restart AURORA to load the settings. The refresh token stays on this machine.")
PY
