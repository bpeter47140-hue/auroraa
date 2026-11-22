#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
mkdir -p "$TEMP/project/scripts" "$TEMP/bin"
cp "$ROOT/scripts/start-public.sh" "$TEMP/project/scripts/start-public.sh"
chmod +x "$TEMP/project/scripts/start-public.sh"

cat > "$TEMP/bin/docker" <<'MOCK'
#!/usr/bin/env bash
printf '%s|%s\n' "$AURORA_DOMAIN" "$*" >> "$DOCKER_CALL_LOG"
exit 0
MOCK
cat > "$TEMP/bin/curl" <<'MOCK'
#!/usr/bin/env bash
exit 0
MOCK
chmod +x "$TEMP/bin/docker" "$TEMP/bin/curl"
export PATH="$TEMP/bin:$PATH"
export DOCKER_CALL_LOG="$TEMP/docker-calls.log"

cat > "$TEMP/project/.env" <<'ENV'
POSTGRES_PASSWORD=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
AURORA_BOOTSTRAP_ENABLED=false
AURORA_DOMAIN=aurora.example.net
AURORA_APP_BASE_URL=https://aurora.example.net
ENV

# A conflicting shell variable must not override the validated .env hostname.
(
  cd "$TEMP/project"
  AURORA_DOMAIN=attacker.example ./scripts/start-public.sh
) > "$TEMP/valid.out"
grep -q 'responding over HTTPS at https://aurora.example.net/' "$TEMP/valid.out"
grep -q '^aurora.example.net|compose --profile public config --quiet$' "$DOCKER_CALL_LOG"
grep -q '^aurora.example.net|compose --profile public up --build -d$' "$DOCKER_CALL_LOG"

# An open first-owner bootstrap must be refused before Docker is called.
: > "$DOCKER_CALL_LOG"
sed -i 's/AURORA_BOOTSTRAP_ENABLED=false/AURORA_BOOTSTRAP_ENABLED=true/' "$TEMP/project/.env"
if (cd "$TEMP/project" && ./scripts/start-public.sh) > "$TEMP/bootstrap.out" 2>&1; then
  echo "Expected bootstrap-enabled deployment to be rejected." >&2
  exit 1
fi
grep -qi 'bootstrap.*not false' "$TEMP/bootstrap.out"
test ! -s "$DOCKER_CALL_LOG"

# An unsafe hostname must be rejected and must not execute shell syntax.
: > "$DOCKER_CALL_LOG"
cat > "$TEMP/project/.env" <<'ENV'
POSTGRES_PASSWORD=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
AURORA_BOOTSTRAP_ENABLED=false
AURORA_DOMAIN=$(touch /tmp/aurora-env-injection-test)
AURORA_APP_BASE_URL=https://aurora.example.net
ENV
rm -f /tmp/aurora-env-injection-test
if (cd "$TEMP/project" && ./scripts/start-public.sh) > "$TEMP/hostname.out" 2>&1; then
  echo "Expected unsafe hostname to be rejected." >&2
  exit 1
fi
grep -q 'real fully-qualified public hostname' "$TEMP/hostname.out"
test ! -e /tmp/aurora-env-injection-test
test ! -s "$DOCKER_CALL_LOG"

echo "Public deployment preflight tests passed: validated host wins over shell override; bootstrap and unsafe hostname are blocked; .env is never sourced."
