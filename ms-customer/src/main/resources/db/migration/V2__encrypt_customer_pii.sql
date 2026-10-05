-- La cédula y el teléfono pasan a guardarse cifrados (AES-256-GCM, ver FieldEncryptor): el texto cifrado es más largo.
ALTER TABLE customers ALTER COLUMN id_number TYPE VARCHAR(255);
ALTER TABLE customers ALTER COLUMN phone TYPE VARCHAR(255);

-- Un índice sobre texto cifrado con IV aleatorio no sirve para buscar: si a futuro hace falta buscar por cédula,
-- se agrega un "blind index" (HMAC de la cédula con otra clave). Ver docs/security.md.
DROP INDEX IF EXISTS idx_customers_id_number;
