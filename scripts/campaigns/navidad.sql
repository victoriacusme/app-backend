-- =====================================================================================
-- Campaña de Navidad: banner nuevo en el home SIN publicar la app (Server-Driven UI).
--
-- Ejecutar (cualquiera de las dos):
--   IntelliJ: clic derecho en este archivo → Run → conexión customer_db (localhost:5434)
--   Terminal: docker compose exec -T customer-db psql -U nexo -d customer_db < scripts/campaigns/navidad.sql
--
-- Luego, en la app: pull-to-refresh en el home. Es reejecutable: reemplaza la campaña si ya existe.
-- =====================================================================================

BEGIN;

DELETE FROM experience_components WHERE name = 'campaign-navidad';

INSERT INTO experience_components
    (id, name, screen, segment, type, position, props, active, promotion, starts_at, ends_at)
VALUES (
    gen_random_uuid(),
    'campaign-navidad',          -- nombre interno único: con él se edita, apaga o borra
    'home',
    NULL,                        -- NULL = todos | 'YOUNG' | 'PREMIUM' | 'ENTREPRENEUR' | 'STANDARD'
    'promo_banner',
    12,                          -- posición: menor = más arriba (saludo = 10, cuentas = 20)
    '{
        "title":    {"es": "Navidad Nexo: transfiere sin comisión",
                     "en": "Nexo Christmas: fee-free transfers"},
        "subtitle": {"es": "Regala tranquilidad hasta el 25 de diciembre",
                     "en": "Give peace of mind until December 25"},
        "imageUrl": "https://t0.gstatic.com/licensed-image?q=tbn:ANd9GcRwKbd8lkdV9j1UV9FCsM3wSUvHbkjYTrM3Wz8njzAYC8Mz4W3ai2oGs5tScEXQUK_c",
        "deeplink": "app://transfer"
     }',
    -- imageUrl: imagen pública 1000x400 (5:2). Para cambiarla usa otra URL o agrega ?v=2 (la app la cachea).
    -- deeplink: "app://transfer" (singular) abre Transferir con ESTE texto. Evita app://transfers,
    --           app://savings, app://advisor y app://credit: la app los reemplaza por sus propios textos.
    true,                        -- activa
    true,                        -- es promoción: respeta la preferencia "Promociones" del cliente
    now(),                       -- desde: ya, para la demo. Fecha real: '2026-12-01 00:00-05'
    '2026-12-26 00:00-05'        -- hasta: desaparece sola al terminar la Navidad (hora de Ecuador)
);

COMMIT;

-- Ver cómo quedó:
SELECT name, coalesce(segment, '(todos)') AS segmento, position, active, starts_at, ends_at,
       props -> 'title' ->> 'es' AS titulo
FROM experience_components
WHERE name = 'campaign-navidad';

-- -------------------------------------------------------------------------------------
-- Cambios rápidos (descomentar y ejecutar la línea que necesites):
--
-- Cambiar la imagen:
--   UPDATE experience_components SET props = jsonb_set(props, '{imageUrl}', '"https://picsum.photos/seed/arbol/1000/400"') WHERE name = 'campaign-navidad';
-- Cambiar textos (|| reemplaza solo estas claves):
--   UPDATE experience_components SET props = props || '{"title": {"es": "Último día de Navidad Nexo", "en": "Last day of Nexo Christmas"}}' WHERE name = 'campaign-navidad';
-- Subirla al primer lugar:
--   UPDATE experience_components SET position = 5 WHERE name = 'campaign-navidad';
-- Solo para jóvenes:
--   UPDATE experience_components SET segment = 'YOUNG' WHERE name = 'campaign-navidad';
-- Apagar / encender:
--   scripts/experience.sh off campaign-navidad      |   scripts/experience.sh on campaign-navidad
-- Quitarla:
--   DELETE FROM experience_components WHERE name = 'campaign-navidad';
-- -------------------------------------------------------------------------------------
