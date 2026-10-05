-- El id del cliente es el customerId que viaja como "sub" en el JWT emitido por ms-auth.
CREATE TABLE customers (
    id          UUID         PRIMARY KEY,
    full_name   VARCHAR(120) NOT NULL,
    id_number   VARCHAR(20)  NOT NULL,
    email       VARCHAR(120) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    birth_date  DATE         NOT NULL,
    segment     VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_customers_segment CHECK (segment IN ('YOUNG', 'PREMIUM', 'ENTREPRENEUR', 'STANDARD'))
);

-- Sin UNIQUE: si un onboarding falla a medias, el reintento llega con otro customerId y no debe quedar bloqueado.
CREATE INDEX idx_customers_id_number ON customers (id_number);

CREATE TABLE preferences (
    customer_id           UUID        PRIMARY KEY REFERENCES customers (id) ON DELETE CASCADE,
    language              VARCHAR(2)  NOT NULL DEFAULT 'es',
    theme                 VARCHAR(10) NOT NULL DEFAULT 'SYSTEM',
    notifications_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    show_promotions       BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_preferences_language CHECK (language IN ('es', 'en')),
    CONSTRAINT ck_preferences_theme CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM'))
);

-- Server-driven UI: cada fila es un componente de una pantalla. Activar o editar una fila cambia la app
-- sin publicar una versión nueva. La app ignora los "type" que no conoce.
CREATE TABLE experience_components (
    id          UUID        PRIMARY KEY,
    name        VARCHAR(60) NOT NULL,
    screen      VARCHAR(30) NOT NULL,
    segment     VARCHAR(20),                       -- NULL: aplica a todos los segmentos
    type        VARCHAR(40) NOT NULL,
    position    INTEGER     NOT NULL,
    props       JSONB       NOT NULL DEFAULT '{}',
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    promotion   BOOLEAN     NOT NULL DEFAULT FALSE, -- se oculta si el cliente desactivó las promociones
    starts_at   TIMESTAMPTZ,                       -- ventana de una campaña (opcional)
    ends_at     TIMESTAMPTZ,
    hour_from   SMALLINT,                          -- franja horaria local [hour_from, hour_to) (opcional)
    hour_to     SMALLINT,
    CONSTRAINT uk_experience_components_name UNIQUE (name),
    CONSTRAINT ck_experience_components_segment
        CHECK (segment IS NULL OR segment IN ('YOUNG', 'PREMIUM', 'ENTREPRENEUR', 'STANDARD')),
    CONSTRAINT ck_experience_components_hours
        CHECK ((hour_from IS NULL AND hour_to IS NULL) OR (hour_from BETWEEN 0 AND 23 AND hour_to BETWEEN 1 AND 24 AND hour_from < hour_to))
);

CREATE INDEX idx_experience_components_screen ON experience_components (screen) WHERE active;
