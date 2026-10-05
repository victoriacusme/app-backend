-- Clientes de prueba (solo desarrollo). Los id coinciden con los customer_id de ms-auth y ms-accounts.
INSERT INTO customers (id, full_name, id_number, email, phone, birth_date, segment)
VALUES ('11111111-1111-1111-1111-111111111111', 'Ana Torres',    '1712345678', 'ana@nexo.ec',    '+593991234567', '2003-05-14', 'YOUNG'),
       ('22222222-2222-2222-2222-222222222222', 'Carlos Mena',   '1709876543', 'carlos@nexo.ec', '+593987654321', '1979-11-02', 'PREMIUM'),
       ('33333333-3333-3333-3333-333333333333', 'Lucía Paredes', '0923456789', 'lucia@nexo.ec',  '+593998877665', '1988-02-21', 'ENTREPRENEUR');

INSERT INTO preferences (customer_id, language, theme, notifications_enabled, show_promotions)
VALUES ('11111111-1111-1111-1111-111111111111', 'es', 'DARK',   TRUE, TRUE),
       ('22222222-2222-2222-2222-222222222222', 'es', 'SYSTEM', TRUE, TRUE),
       ('33333333-3333-3333-3333-333333333333', 'es', 'LIGHT',  TRUE, TRUE);

-- Home por segmento. {greeting} y {firstName} los reemplaza el ExperienceComposer.
INSERT INTO experience_components (id, name, screen, segment, type, position, props, promotion, active)
VALUES
-- Comunes a todos los segmentos
('e0000000-0000-0000-0000-000000000001', 'greeting', 'home', NULL, 'greeting', 10,
 '{"title": "{greeting}, {firstName}"}', FALSE, TRUE),
('e0000000-0000-0000-0000-000000000002', 'accounts-summary', 'home', NULL, 'accounts_summary', 20,
 '{"title": "Tus cuentas", "showTotal": true}', FALSE, TRUE),

-- YOUNG (ana): metas de ahorro y recargas
('e0000000-0000-0000-0000-000000000011', 'young-quick-actions', 'home', 'YOUNG', 'quick_actions', 30,
 '{"actions": [{"id": "transfer", "label": "Transferir", "icon": "swap", "deeplink": "app://transfers"},
               {"id": "topup", "label": "Recargar celular", "icon": "phone", "deeplink": "app://topups"},
               {"id": "goals", "label": "Mis metas", "icon": "target", "deeplink": "app://savings"}]}', FALSE, TRUE),
('e0000000-0000-0000-0000-000000000012', 'young-savings-goal', 'home', 'YOUNG', 'savings_goal', 40,
 '{"title": "Meta: viaje a Galápagos", "accountId": "c0000000-0000-0000-0000-000000000012", "target": "1000.00", "deeplink": "app://accounts/c0000000-0000-0000-0000-000000000012"}', FALSE, TRUE),
('e0000000-0000-0000-0000-000000000013', 'young-promo-first-goal', 'home', 'YOUNG', 'promo_banner', 50,
 '{"title": "Gana 5% extra en tu primera meta", "subtitle": "Solo este mes", "imageUrl": "https://picsum.photos/seed/nexo-young/600/240", "deeplink": "app://savings"}', TRUE, TRUE),

-- PREMIUM (carlos): inversiones y tipo de cambio
('e0000000-0000-0000-0000-000000000021', 'premium-quick-actions', 'home', 'PREMIUM', 'quick_actions', 30,
 '{"actions": [{"id": "transfer", "label": "Transferir", "icon": "swap", "deeplink": "app://transfers"},
               {"id": "invest", "label": "Inversiones", "icon": "chart", "deeplink": "app://investments"},
               {"id": "advisor", "label": "Mi asesor", "icon": "person", "deeplink": "app://advisor"}]}', FALSE, TRUE),
('e0000000-0000-0000-0000-000000000022', 'premium-fx-rates', 'home', 'PREMIUM', 'fx_rates', 40,
 '{"title": "Tipo de cambio", "base": "USD", "symbols": ["EUR", "COP", "PEN", "MXN"]}', FALSE, TRUE),
('e0000000-0000-0000-0000-000000000023', 'premium-promo-advisory', 'home', 'PREMIUM', 'promo_banner', 50,
 '{"title": "Asesoría de inversiones sin costo", "subtitle": "Agenda con tu asesor", "imageUrl": "https://picsum.photos/seed/nexo-premium/600/240", "deeplink": "app://advisor"}', TRUE, TRUE),

-- ENTREPRENEUR (lucia): cobros y negocio
('e0000000-0000-0000-0000-000000000031', 'entrepreneur-quick-actions', 'home', 'ENTREPRENEUR', 'quick_actions', 30,
 '{"actions": [{"id": "transfer", "label": "Transferir", "icon": "swap", "deeplink": "app://transfers"},
               {"id": "collect", "label": "Cobrar con QR", "icon": "qr", "deeplink": "app://collect"},
               {"id": "suppliers", "label": "Pagar proveedores", "icon": "store", "deeplink": "app://suppliers"}]}', FALSE, TRUE),
('e0000000-0000-0000-0000-000000000032', 'entrepreneur-promo-credit', 'home', 'ENTREPRENEUR', 'promo_banner', 40,
 '{"title": "Crédito para tu negocio", "subtitle": "Pre-aprobado hasta $5.000", "imageUrl": "https://picsum.photos/seed/nexo-business/600/240", "deeplink": "app://credit"}', TRUE, TRUE),
-- Tipo que la app todavía no conoce: debe ignorarlo sin romperse (demostración de compatibilidad hacia adelante).
('e0000000-0000-0000-0000-000000000033', 'entrepreneur-cash-flow', 'home', 'ENTREPRENEUR', 'cash_flow_chart', 50,
 '{"title": "Flujo de caja del mes"}', FALSE, TRUE),

-- STANDARD (clientes nuevos adultos)
('e0000000-0000-0000-0000-000000000041', 'standard-quick-actions', 'home', 'STANDARD', 'quick_actions', 30,
 '{"actions": [{"id": "transfer", "label": "Transferir", "icon": "swap", "deeplink": "app://transfers"},
               {"id": "profile", "label": "Mi perfil", "icon": "person", "deeplink": "app://profile"}]}', FALSE, TRUE),

-- Campaña apagada: para la demo, activarla cambia el home de todos sin publicar la app (scripts/experience.sh).
('e0000000-0000-0000-0000-000000000099', 'campaign-black-friday', 'home', NULL, 'promo_banner', 15,
 '{"title": "Black Friday Nexo", "subtitle": "0% de comisión en todas tus transferencias", "imageUrl": "https://picsum.photos/seed/nexo-bf/600/240", "deeplink": "app://transfers"}', TRUE, FALSE);
