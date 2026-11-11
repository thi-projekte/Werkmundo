#!/usr/bin/env bash
# Schneller Rauchtest gegen den lokal laufenden ai-service.
# Aufruf:  ./beispiele/test-lokal.sh            (Standard: http://localhost:8086)
#          AI_URL=http://localhost:9000 ./beispiele/test-lokal.sh
set -euo pipefail
AI_URL="${AI_URL:-http://localhost:8086}"
DIR="$(cd "$(dirname "$0")" && pwd)"

echo "== Health";                       curl -fsS "$AI_URL/q/health/ready"; echo
echo "== /ai/analyze Erstangebot";      curl -fsS -X POST "$AI_URL/ai/analyze" -H 'Content-Type: application/json' -d @"$DIR/erstangebot.json"; echo
echo "== /ai/analyze Korrektur";        curl -fsS -X POST "$AI_URL/ai/analyze" -H 'Content-Type: application/json' -d @"$DIR/korrektur.json"; echo
echo "== /ai/process (202, Ergebnis geht an die Process Engine)"
curl -sS -o /dev/stdout -w '\nHTTP %{http_code}\n' -X POST "$AI_URL/ai/process" -H 'Content-Type: application/json' -H 'X-Handwerker-Id: lokal-test' -d @"$DIR/erstangebot.json"
echo "== Ungueltiger Payload (400 erwartet)"
curl -sS -o /dev/stdout -w '\nHTTP %{http_code}\n' -X POST "$AI_URL/ai/process" -H 'Content-Type: application/json' -d '{"businessKey":"BK-X"}'
