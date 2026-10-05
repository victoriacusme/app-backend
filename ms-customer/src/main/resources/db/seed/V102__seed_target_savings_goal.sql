-- Corrección de la semilla V100: la meta de ahorro apunta a una cuenta de ana, así que debe ser solo de ana
-- y no de todo el segmento YOUNG (un cliente joven nuevo la recibía con una cuenta ajena).
UPDATE experience_components
SET customer_id = '11111111-1111-1111-1111-111111111111'
WHERE name = 'young-savings-goal';
