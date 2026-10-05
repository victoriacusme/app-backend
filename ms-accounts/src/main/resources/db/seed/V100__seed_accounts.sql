-- Cuentas y movimientos de prueba (solo desarrollo). Los customer_id coinciden con los usuarios de ms-auth:
--   ana    -> 11111111-1111-1111-1111-111111111111  (YOUNG)
--   carlos -> 22222222-2222-2222-2222-222222222222  (PREMIUM)
--   lucia  -> 33333333-3333-3333-3333-333333333333  (ENTREPRENEUR)
-- Los saldos no se escriben a mano: se calculan al final a partir de los movimientos.
INSERT INTO accounts (id, customer_id, number, type, currency, balance, status, alias, is_default)
VALUES ('c0000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', '2200014521', 'SAVINGS',  'USD', 0, 'ACTIVE', 'Ahorros',       TRUE),
       ('c0000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', '2200017834', 'SAVINGS',  'USD', 0, 'ACTIVE', 'Meta: viaje',   FALSE),
       ('c0000000-0000-0000-0000-000000000021', '22222222-2222-2222-2222-222222222222', '2200020117', 'CHECKING', 'USD', 0, 'ACTIVE', 'Corriente',     TRUE),
       ('c0000000-0000-0000-0000-000000000022', '22222222-2222-2222-2222-222222222222', '2200023390', 'SAVINGS',  'USD', 0, 'ACTIVE', 'Ahorros',       FALSE),
       ('c0000000-0000-0000-0000-000000000023', '22222222-2222-2222-2222-222222222222', '2200026652', 'SAVINGS',  'USD', 0, 'ACTIVE', 'Inversiones',   FALSE),
       ('c0000000-0000-0000-0000-000000000031', '33333333-3333-3333-3333-333333333333', '2200031008', 'CHECKING', 'USD', 0, 'ACTIVE', 'Negocio',       TRUE),
       ('c0000000-0000-0000-0000-000000000032', '33333333-3333-3333-3333-333333333333', '2200034276', 'SAVINGS',  'USD', 0, 'ACTIVE', 'Personal',      FALSE);

-- Depósito de apertura: el movimiento más antiguo de cada cuenta, para que el saldo nunca quede negativo.
INSERT INTO movements (id, account_id, type, amount, balance_after, description, booked_at)
SELECT md5('opening-' || a.id)::uuid, a.id, 'CREDIT', v.amount, 0, 'Depósito de apertura', now() - interval '120 days'
FROM accounts a
JOIN (VALUES ('c0000000-0000-0000-0000-000000000011'::uuid, 800.00),
             ('c0000000-0000-0000-0000-000000000012'::uuid, 150.00),
             ('c0000000-0000-0000-0000-000000000021'::uuid, 5000.00),
             ('c0000000-0000-0000-0000-000000000022'::uuid, 12000.00),
             ('c0000000-0000-0000-0000-000000000023'::uuid, 25000.00),
             ('c0000000-0000-0000-0000-000000000031'::uuid, 3000.00),
             ('c0000000-0000-0000-0000-000000000032'::uuid, 600.00)) AS v (account_id, amount)
  ON v.account_id = a.id;

-- Movimientos recurrentes generados de forma determinista (ids estables entre ejecuciones).
-- Cada fila: cuenta, cantidad de movimientos, cada cuántos días, ingreso periódico y su descripción.
INSERT INTO movements (id, account_id, type, amount, balance_after, description, booked_at)
SELECT md5(p.account_id || '-' || n)::uuid,
       p.account_id,
       CASE WHEN n % p.credit_every = 0 THEN 'CREDIT' ELSE 'DEBIT' END,
       CASE WHEN n % p.credit_every = 0 THEN p.credit_amount
            ELSE round((p.debit_base + (n * 37 % 23) * p.debit_step)::numeric, 2) END,
       0,
       CASE WHEN n % p.credit_every = 0 THEN p.credit_description
            ELSE (p.debit_descriptions)[1 + n % array_length(p.debit_descriptions, 1)] END,
       now() - interval '118 days' + n * p.spacing
FROM (VALUES
        ('c0000000-0000-0000-0000-000000000011'::uuid, 45, interval '2 days 13 hours', 6, 450.00,  'Pago de nómina',
         4.50, 1.35, ARRAY['Supermercado Tía', 'Recarga celular', 'Uber', 'Spotify', 'Cafetería', 'Farmacia']),
        ('c0000000-0000-0000-0000-000000000012'::uuid, 8,  interval '14 days',          2, 75.00,   'Aporte a meta de ahorro',
         10.00, 0.50, ARRAY['Reserva de vuelo', 'Seguro de viaje']),
        ('c0000000-0000-0000-0000-000000000021'::uuid, 60, interval '1 day 22 hours',   8, 3200.00, 'Pago de nómina',
         12.00, 6.10, ARRAY['Supermaxi', 'Restaurante', 'Gasolinera', 'Netflix', 'Pago tarjeta de crédito', 'Servicios básicos', 'Amazon']),
        ('c0000000-0000-0000-0000-000000000022'::uuid, 10, interval '11 days',          2, 500.00,  'Transferencia recibida',
         40.00, 9.00, ARRAY['Retiro cajero', 'Pago de seguro']),
        ('c0000000-0000-0000-0000-000000000023'::uuid, 4,  interval '28 days',          1, 210.40,  'Rendimiento de inversión',
         0, 0, ARRAY['-']),
        ('c0000000-0000-0000-0000-000000000031'::uuid, 50, interval '2 days 8 hours',   3, 380.00,  'Cobro a cliente',
         20.00, 7.40, ARRAY['Proveedor Distribuidora Andina', 'Pago SRI', 'Internet negocio', 'Mantenimiento', 'Envío courier']),
        ('c0000000-0000-0000-0000-000000000032'::uuid, 12, interval '9 days',           4, 250.00,  'Retiro de utilidades',
         8.00, 2.10, ARRAY['Supermercado', 'Farmacia', 'Gimnasio'])
     ) AS p (account_id, total, spacing, credit_every, credit_amount, credit_description, debit_base, debit_step, debit_descriptions)
CROSS JOIN LATERAL generate_series(1, p.total) AS n;

-- Saldo posterior de cada movimiento (acumulado en orden cronológico) y saldo final de cada cuenta.
UPDATE movements m
SET balance_after = s.running
FROM (SELECT id,
             SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END)
                 OVER (PARTITION BY account_id ORDER BY booked_at, id) AS running
      FROM movements) s
WHERE m.id = s.id;

UPDATE accounts a
SET balance = COALESCE((SELECT SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END)
                        FROM movements m WHERE m.account_id = a.id), 0);
