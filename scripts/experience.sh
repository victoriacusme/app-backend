#!/usr/bin/env bash
# Cambia la experiencia server-driven en vivo, directamente en la BD de ms-customer (sin publicar la app).
# Uso:
#   scripts/experience.sh list                      componentes del home y su estado
#   scripts/experience.sh on  campaign-black-friday activa un componente
#   scripts/experience.sh off campaign-black-friday lo desactiva
set -euo pipefail
cd "$(dirname "$0")/.."

psql() {
  docker compose exec -T customer-db psql -U "${DB_USER:-nexo}" -d customer_db -v ON_ERROR_STOP=1 "$@"
}

action="${1:-list}"
case "$action" in
  list)
    psql -c "SELECT name, coalesce(segment, '(todos)') AS segmento, type, position AS pos,
                    CASE WHEN active THEN 'activo' ELSE '-' END AS estado, props->>'title' AS titulo
             FROM experience_components WHERE screen = 'home' ORDER BY segment NULLS FIRST, position"
    ;;
  on|off)
    name="${2:?Falta el nombre del componente (ver: scripts/experience.sh list)}"
    active=$([[ "$action" == "on" ]] && echo TRUE || echo FALSE)
    updated=$(psql -tA -v name="$name" <<SQL
UPDATE experience_components SET active = $active WHERE name = :'name' RETURNING name;
SQL
)
    [[ -n "$updated" ]] || { echo "No existe el componente '$name'" >&2; exit 1; }
    echo "✅ $name: $([[ "$action" == "on" ]] && echo activado || echo desactivado). La app lo verá en su próxima carga del home."
    ;;
  *)
    echo "Uso: $0 list | on <nombre> | off <nombre>" >&2; exit 1 ;;
esac
