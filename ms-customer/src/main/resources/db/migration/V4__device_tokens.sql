-- Tokens de push (FCM) de los dispositivos del cliente. Un token identifica a una instalación de la app:
-- si en ese teléfono inicia sesión otro cliente, el token se reasigna (clave primaria = token).
CREATE TABLE device_tokens (
    token       VARCHAR(512) PRIMARY KEY,
    customer_id UUID         NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    platform    VARCHAR(10)  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_device_tokens_platform CHECK (platform IN ('ANDROID', 'IOS'))
);

CREATE INDEX idx_device_tokens_customer ON device_tokens (customer_id);
