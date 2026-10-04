#!/usr/bin/env bash
# Funciones compartidas por los scripts de caos. Hablan con la API HTTP de Toxiproxy (sin instalar su CLI).
set -euo pipefail

TOXIPROXY_URL="${TOXIPROXY_URL:-http://localhost:8474}"

# Acepta "accounts", "ms-accounts", "auth", "customer"...
proxy_name() {
  local service="${1:-}"
  case "$service" in
    auth|ms-auth) echo "ms-auth" ;;
    customer|ms-customer) echo "ms-customer" ;;
    accounts|ms-accounts) echo "ms-accounts" ;;
    *) echo "Servicio desconocido: '$service'. Usa: auth | customer | accounts" >&2; exit 1 ;;
  esac
}

toxiproxy() {
  local method="$1" path="$2" body="${3:-}"
  local args=(-sS --fail-with-body -X "$method" "$TOXIPROXY_URL$path")
  [[ -n "$body" ]] && args+=(-H 'Content-Type: application/json' -d "$body")
  curl "${args[@]}" >/dev/null
}
