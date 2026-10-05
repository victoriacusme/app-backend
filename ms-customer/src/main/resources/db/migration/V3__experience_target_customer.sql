-- Componentes dirigidos a un cliente concreto (p. ej. su meta de ahorro, que apunta a una cuenta suya).
-- NULL: el componente aplica a todos los clientes del segmento.
ALTER TABLE experience_components ADD COLUMN customer_id UUID REFERENCES customers (id) ON DELETE CASCADE;
