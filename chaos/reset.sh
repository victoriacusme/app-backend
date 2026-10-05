#!/usr/bin/env bash
# Restaura todo: vuelve a habilitar los micros y elimina la latencia.
# Uso: chaos/reset.sh
source "$(dirname "$0")/_common.sh"

toxiproxy POST "/reset"
echo "✅ Toxiproxy restaurado: todos los micros arriba y sin latencia"
