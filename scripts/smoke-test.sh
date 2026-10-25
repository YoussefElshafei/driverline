#!/usr/bin/env bash
# Smoke test for a running DriverLine stack (docker compose up). Calls the app through nginx, the same way a
# browser does, and checks that each part answers. Used by CI; also runnable locally from Git Bash:
#   bash scripts/smoke-test.sh                # defaults to http://localhost:8088
#   bash scripts/smoke-test.sh http://localhost:8089
set -euo pipefail

BASE_URL="${1:-http://localhost:8088}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
failures=0

# check <description> <expected text> <curl arguments...>
check() {
  local description="$1" expected="$2"
  shift 2
  local body
  if body="$(curl -sS --max-time 200 "$@")" && grep -q -- "$expected" <<<"$body"; then
    echo "PASS  $description"
  else
    echo "FAIL  $description (expected to find: $expected)"
    echo "      got: ${body:0:300}"
    failures=$((failures + 1))
  fi
}

check "frontend app shell"                 'id="root"'                 "$BASE_URL/"
check "client-side route served by nginx"   'id="root"'                 "$BASE_URL/forecast"
check "variance report through /api"        '"latestActualMonth":9'     "$BASE_URL/api/variance/by-department?year=2026"
check "forecast scenarios"                  '"scenarioCode":"WORST"'    "$BASE_URL/api/forecast/compare?year=2026"
check "budget grid"                         '"accountCode":"4000"'      "$BASE_URL/api/budgets?year=2026&department=SALES"
check "OpenAPI docs through nginx"          '"openapi"'                 "$BASE_URL/v3/api-docs"
# Rule-based in CI (no Ollama there); written by the language model when Ollama is running locally.
check "commentary via commentary-service"   '"provider":'               "$BASE_URL/api/variance/commentary?year=2026"
# The file has deliberate mistakes: the import must be rejected with row errors and save nothing.
check "actuals import validation (422)"     '"Import failed"' \
  -F "file=@$SCRIPT_DIR/../samples/actuals-with-errors.csv" "$BASE_URL/api/actuals/import"

if [[ $failures -gt 0 ]]; then
  echo "$failures check(s) failed"
  exit 1
fi
echo "All smoke checks passed against $BASE_URL"
