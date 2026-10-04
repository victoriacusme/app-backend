-- Usuarios de prueba (solo desarrollo). Contraseña de los tres: Nexo2026*
-- Los customer_id son fijos: ms-customer y ms-accounts siembran sus datos con los mismos valores.
--   ana    -> 11111111-1111-1111-1111-111111111111  (YOUNG)
--   carlos -> 22222222-2222-2222-2222-222222222222  (PREMIUM)
--   lucia  -> 33333333-3333-3333-3333-333333333333  (ENTREPRENEUR)
INSERT INTO users (id, username, customer_id, password_hash, status, failed_attempts)
VALUES ('a0000000-0000-0000-0000-000000000001', 'ana', '11111111-1111-1111-1111-111111111111',
        '$argon2id$v=19$m=16384,t=2,p=1$qOevmbQlS8KJ+16nSOEWDg$vcYzq/sVUc0P9DNwJ5Oe2DLo6bpyisGFfr+4LM6SzUU',
        'ACTIVE', 0),
       ('a0000000-0000-0000-0000-000000000002', 'carlos', '22222222-2222-2222-2222-222222222222',
        '$argon2id$v=19$m=16384,t=2,p=1$x2DZo5mRPYrAvht1YB5N1A$GosPPFVW6rJkSrCt0l5bajIOwXlsm3A6LtRoZAWwJCg',
        'ACTIVE', 0),
       ('a0000000-0000-0000-0000-000000000003', 'lucia', '33333333-3333-3333-3333-333333333333',
        '$argon2id$v=19$m=16384,t=2,p=1$DQ8AS1qnM/zo9Npo3gCcLA$pyS2s2AYyJpnkmRkkCe/NKbfGz6Orqzb2vZ1lry95BQ',
        'ACTIVE', 0)
ON CONFLICT (username) DO NOTHING;
