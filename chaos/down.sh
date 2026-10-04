#!/usr/bin/env bash
# Corta la conexión hacia un micro: el gateway responde 503 service-unavailable.
# Uso: chaos/down.sh <auth|customer|accounts>
source "$(dirname "$0")/_common.sh"

proxy="$(proxy_name "${1:-}")"
toxiproxy POST "/proxies/$proxy" '{"enabled":false}'
echo "🔌 $proxy: caído"
