CREATE SEQUENCE account_number_seq START WITH 2200100000;

CREATE TABLE accounts (
    id          UUID          PRIMARY KEY,
    customer_id UUID          NOT NULL,
    number      VARCHAR(20)   NOT NULL,
    type        VARCHAR(20)   NOT NULL,
    currency    VARCHAR(3)    NOT NULL,
    balance     NUMERIC(19, 2) NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    alias       VARCHAR(50),
    is_default  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_accounts_number UNIQUE (number),
    CONSTRAINT ck_accounts_type CHECK (type IN ('SAVINGS', 'CHECKING')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT ck_accounts_balance CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_customer ON accounts (customer_id);
-- Una sola cuenta por defecto por cliente: hace idempotente la apertura del onboarding.
CREATE UNIQUE INDEX uk_accounts_default_per_customer ON accounts (customer_id) WHERE is_default;

CREATE TABLE movements (
    id            UUID           PRIMARY KEY,
    account_id    UUID           NOT NULL REFERENCES accounts (id),
    type          VARCHAR(10)    NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    description   VARCHAR(140)   NOT NULL,
    booked_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_movements_type CHECK (type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_movements_amount CHECK (amount > 0)
);

-- Soporta la paginación por cursor (booked_at, id) en orden descendente.
CREATE INDEX idx_movements_account_booked ON movements (account_id, booked_at DESC, id DESC);
