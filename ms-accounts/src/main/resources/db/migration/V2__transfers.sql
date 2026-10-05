CREATE TABLE transfers (
    id                UUID           PRIMARY KEY,
    customer_id       UUID           NOT NULL,
    idempotency_key   VARCHAR(64)    NOT NULL,
    -- Huella SHA-256 del contenido: detecta una misma Idempotency-Key reutilizada con otros datos.
    request_hash      VARCHAR(64)    NOT NULL,
    source_account_id UUID           NOT NULL REFERENCES accounts (id),
    target_account_id UUID           NOT NULL REFERENCES accounts (id),
    amount            NUMERIC(19, 2) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    description       VARCHAR(100),
    status            VARCHAR(20)    NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL,
    -- La clave es única por cliente: dos clientes pueden generar el mismo valor sin chocar.
    CONSTRAINT uk_transfers_idempotency UNIQUE (customer_id, idempotency_key),
    CONSTRAINT ck_transfers_amount CHECK (amount > 0),
    CONSTRAINT ck_transfers_distinct_accounts CHECK (source_account_id <> target_account_id),
    CONSTRAINT ck_transfers_status CHECK (status IN ('COMPLETED'))
);

ALTER TABLE movements ADD COLUMN transfer_id UUID REFERENCES transfers (id);
