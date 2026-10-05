#!/usr/bin/env bash
# Agrega latencia a las respuestas de un micro.
# Uso: chaos/latency.sh <auth|customer|accounts> [milisegundos=3000] [jitter=0]
source "$(dirname "$0")/_common.sh"

proxy="$(proxy_name "${1:-}")"
latency="${2:-3000}"
jitter="${3:-0}"
attributes="{\"latency\":$latency,\"jitter\":$jitter}"

# Si ya existe el toxic se actualiza; si no, se crea.
if ! toxiproxy POST "/proxies/$proxy/toxics/latency" "{\"attributes\":$attributes}" 2>/dev/null; then
  toxiproxy POST "/proxies/$proxy/toxics" \
    "{\"name\":\"latency\",\"type\":\"latency\",\"stream\":\"downstream\",\"toxicity\":1,\"attributes\":$attributes}"
fi
echo "🐢 $proxy: +${latency} ms de latencia (jitter ${jitter} ms)"
