#!/usr/bin/env bash
# Muestra el estado de cada micro detrás de Toxiproxy.
# Uso: chaos/status.sh
source "$(dirname "$0")/_common.sh"

curl -sS "$TOXIPROXY_URL/proxies" | python3 -c "$(cat <<'PY'
import json, sys
for name, proxy in sorted(json.load(sys.stdin).items()):
    state = "arriba" if proxy["enabled"] else "CAÍDO"
    toxics = ", ".join("%s %s" % (t["type"], t["attributes"]) for t in proxy["toxics"]) or "sin toxics"
    print("%-12s %-7s %s" % (name, state, toxics))
PY
)"
