# Arquitectura

Vista general del backend de Nexo Bank y de sus flujos críticos. Las razones de cada decisión están en
[`decisions.md`](decisions.md); los riesgos y el despliegue en producción, en
[`risks-and-scaling.md`](risks-and-scaling.md).

## 1. Componentes

```mermaid
flowchart TB
    app["📱 App Flutter"]

    subgraph edge["Borde"]
        gw["Gateway Nginx :8080<br/>ruteo · rate limit · timeouts<br/>correlation-id · bloquea /internal"]
        tp["Toxiproxy<br/>latencia y caídas simuladas"]
    end

    subgraph services["Microservicios (Spring Boot 3.5 · Java 21 · hexagonal)"]
        auth["ms-auth :8081<br/>login · JWT · refresh · JWKS<br/>onboarding"]
        cust["ms-customer :8082<br/>perfil · preferencias<br/>experiencia SDUI · push"]
        acc["ms-accounts :8083<br/>cuentas · movimientos<br/>transferencias"]
    end

    subgraph data["Datos (PostgreSQL 16, una base por servicio)"]
        adb[("auth_db")]
        cdb[("customer_db")]
        accdb[("accounts_db")]
    end

    fx["🌐 open.er-api.com<br/>tipo de cambio"]
    fcm["🔔 Firebase Cloud Messaging"]

    app -->|"HTTPS · JWT · JWE en el login"| gw
    gw --> tp
    gw -->|"/external/fx (sin token del cliente)"| fx
    tp --> auth & cust & acc
    auth --> adb
    cust --> cdb
    acc --> accdb

    auth -.->|"onboarding · API key"| cust
    auth -.->|"onboarding · API key"| acc
    acc -.->|"aviso de transferencia · API key"| cust
    cust -->|"push"| fcm
```

- **Línea continua:** tráfico de la app.
- **Línea punteada:** llamadas entre servicios por la red interna. Las rutas `/internal/**` exigen API key y el
  gateway no las expone.
- **Validación de tokens:** ms-customer y ms-accounts validan el JWT localmente con las claves públicas de ms-auth
  (JWKS), que guardan en caché. No llaman a ms-auth en cada petición.
- **Datos compartidos:** ningún servicio lee la base de datos de otro. Se relacionan solo por el `customerId`, que
  viaja como `sub` en el JWT.

## 2. Capas dentro de cada servicio (hexagonal)

```mermaid
flowchart LR
    subgraph in["Adaptadores de entrada"]
        web["Controllers REST<br/>DTOs · ProblemDetail"]
        internal["Controllers /internal<br/>API key"]
    end

    subgraph app["Aplicación"]
        uc["Casos de uso<br/>@Transactional"]
        ports["Puertos de salida<br/>(interfaces)"]
    end

    subgraph dom["Dominio (sin frameworks)"]
        model["Modelo y reglas<br/>Account.debit · ExperienceComposer<br/>User.registerFailedAttempt"]
    end

    subgraph out["Adaptadores de salida"]
        jpa["JPA / PostgreSQL"]
        http["RestClient a otros servicios"]
        push["FCM o log"]
        crypto["Cifrado AES-GCM"]
    end

    web & internal --> uc
    uc --> model
    uc --> ports
    jpa & http & push -.->|"implementan"| ports
    jpa --- crypto
```

Las dependencias apuntan hacia el dominio: el dominio no conoce Spring, JPA ni HTTP. Por eso los casos de uso se
prueban con Mockito, y los adaptadores aparte, contra PostgreSQL real.

## 3. Modelo de datos

Cada base pertenece a un solo servicio. El `customer_id` se repite entre bases como **referencia lógica**, sin llave
foránea entre servicios.

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : "tiene"
    USERS {
        uuid id PK
        string username UK
        uuid customer_id UK "sub del JWT"
        string password_hash "Argon2id"
        string status "ACTIVE o LOCKED"
        int failed_attempts
    }
    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        string token_hash UK "SHA-256, nunca el token"
        string device_id
        timestamp expires_at
        boolean revoked
    }
```

<p align="center"><em>auth_db (ms-auth)</em></p>

```mermaid
erDiagram
    CUSTOMERS ||--|| PREFERENCES : "tiene"
    CUSTOMERS ||--o{ DEVICE_TOKENS : "registra"
    CUSTOMERS ||--o{ EXPERIENCE_COMPONENTS : "dirigidos a"
    CUSTOMERS {
        uuid id PK "= customer_id"
        string full_name
        string id_number "cifrado v1:..."
        string phone "cifrado v1:..."
        string email
        date birth_date
        string segment "YOUNG PREMIUM ENTREPRENEUR STANDARD"
    }
    PREFERENCES {
        uuid customer_id PK
        string language "es o en"
        string theme
        boolean notifications_enabled
        boolean show_promotions
    }
    DEVICE_TOKENS {
        string token PK "token FCM"
        uuid customer_id FK
        string platform "ANDROID o IOS"
    }
    EXPERIENCE_COMPONENTS {
        uuid id PK
        string name UK
        string screen
        string segment "nulo = todos"
        uuid customer_id FK "nulo = todos"
        string type "greeting, fx_rates..."
        int position
        jsonb props
        boolean active
        boolean promotion
        timestamp starts_at
        timestamp ends_at
    }
```

<p align="center"><em>customer_db (ms-customer)</em></p>

```mermaid
erDiagram
    ACCOUNTS ||--o{ MOVEMENTS : "registra"
    ACCOUNTS ||--o{ TRANSFERS : "origen o destino"
    TRANSFERS ||--|{ MOVEMENTS : "genera 2"
    ACCOUNTS {
        uuid id PK
        uuid customer_id "dueño"
        string number UK "se expone enmascarado"
        string type "SAVINGS o CHECKING"
        decimal balance "CHECK mayor o igual a 0"
        string status
        boolean is_default "una por cliente"
    }
    MOVEMENTS {
        uuid id PK
        uuid account_id FK
        string type "DEBIT o CREDIT"
        decimal amount
        decimal balance_after
        timestamp booked_at "índice para el cursor"
        uuid transfer_id FK
    }
    TRANSFERS {
        uuid id PK
        uuid customer_id
        string idempotency_key "UNIQUE con customer_id"
        string request_hash "huella del contenido"
        uuid source_account_id FK
        uuid target_account_id FK
        decimal amount
        string status
    }
```

<p align="center"><em>accounts_db (ms-accounts)</em></p>

## 4. Flujos críticos

### 4.1 Login y renovación de sesión

```mermaid
sequenceDiagram
    autonumber
    participant App
    participant GW as Gateway
    participant Auth as ms-auth
    participant Acc as ms-accounts

    App->>GW: GET /.well-known/jwks.json
    GW->>Auth: (proxy)
    Auth-->>App: claves públicas sig y enc
    Note over App: cifra usuario y contraseña<br/>con la clave enc (JWE)
    App->>GW: POST /auth/login (application/jose)
    GW->>Auth: (rate limit 20 por min por IP)
    Auth->>Auth: descifra JWE, busca el usuario FOR UPDATE<br/>y verifica Argon2id
    alt contraseña correcta
        Auth->>Auth: firma JWT RS256 de 15 min<br/>y guarda el SHA-256 del refresh token
        Auth-->>App: 200 accessToken y refreshToken
    else contraseña incorrecta
        Auth->>Auth: intentos fallidos + 1
        Auth-->>App: 401 invalid-credentials, o 423 user-locked al quinto intento
    end

    App->>GW: GET /accounts con Bearer JWT
    GW->>Acc: (proxy)
    Acc->>Acc: valida firma, expiración y emisor<br/>con el JWKS en caché, sin llamar a ms-auth
    Acc-->>App: 200 cuentas del sub del token

    Note over App,Auth: a los 15 min el access token expira
    App->>Auth: POST /auth/refresh con el refresh token
    alt token vigente
        Auth->>Auth: revoca el token usado y emite un par nuevo
        Auth-->>App: 200 tokens nuevos
    else token ya rotado (posible robo)
        Auth->>Auth: revoca TODAS las sesiones del usuario
        Auth-->>App: 401 invalid-refresh-token
    end
```

### 4.2 Transferencia entre cuentas propias

```mermaid
sequenceDiagram
    autonumber
    participant App
    participant GW as Gateway
    participant Acc as ms-accounts
    participant DB as accounts_db
    participant Cust as ms-customer
    participant FCM

    Note over App: genera una Idempotency-Key<br/>por cada intento del usuario
    App->>GW: POST /transfers/own<br/>Idempotency-Key y monto "125.50"
    GW->>Acc: (timeout 10 s, sin reintentos)
    Acc->>DB: BEGIN
    Acc->>DB: ¿existe la clave para este cliente?
    alt ya existe con el mismo contenido
        Acc-->>App: 200 transferencia original<br/>Idempotent-Replayed: true
    else ya existe con otro contenido
        Acc-->>App: 409 idempotency-key-reused
    else clave nueva
        Acc->>DB: SELECT ambas cuentas FOR UPDATE ORDER BY id
        Note over Acc,DB: orden fijo: dos transferencias<br/>cruzadas no se bloquean entre sí
        Acc->>Acc: ¿son del cliente? ¿activas?<br/>¿misma moneda? ¿hay saldo?
        alt alguna regla falla
            Acc->>DB: ROLLBACK
            Acc-->>App: 403 account-not-owned o 422 insufficient-funds
        else todo válido
            Acc->>DB: vuelve a verificar la clave con las cuentas ya bloqueadas
            Acc->>DB: INSERT transferencia, UPDATE saldos,<br/>INSERT 2 movimientos
            Acc->>DB: COMMIT
            Acc-->>App: 201 Created
            Acc-)Cust: después del commit y en segundo plano<br/>POST /internal/notifications
            Cust->>Cust: ¿notificaciones activas?<br/>texto en el idioma del cliente
            Cust-)FCM: un envío por dispositivo
        end
    end
```

**Si la app no recibe la respuesta** (timeout o corte de red), reenvía la **misma** solicitud con la **misma** clave.
Nunca se mueve dinero dos veces, y recibe el resultado original.

### 4.3 Onboarding con compensación

```mermaid
sequenceDiagram
    autonumber
    participant App
    participant Auth as ms-auth
    participant Cust as ms-customer
    participant Acc as ms-accounts

    App->>Auth: POST /auth/register (vía gateway)
    Auth->>Auth: ¿el username existe? → 409 username-taken
    Auth->>Auth: genera un customerId nuevo
    Auth->>Cust: POST /internal/customers (API key)
    Cust->>Cust: cifra cédula y teléfono, crea cliente<br/>y preferencias, asigna segmento
    Cust-->>Auth: 201
    Auth->>Acc: POST /internal/accounts (API key)
    alt cuenta abierta
        Acc-->>Auth: 201 cuenta de ahorros
        Auth->>Auth: crea credenciales (Argon2id) y abre sesión
        Auth-->>App: 201 tokens
    else ms-accounts falla
        Acc--xAuth: error o timeout
        Auth->>Cust: DELETE /internal/customers/{id} (compensación)
        Auth-->>App: 503 onboarding-unavailable
        Note over App,Auth: no quedan credenciales ni cliente huérfano<br/>reintentar es seguro (otro customerId)
    end
```

Las credenciales se crean **al final**: si algo falla antes, nunca queda un usuario que pueda iniciar sesión sin
cliente ni cuenta.

### 4.4 Experiencia dinámica (SDUI)

```mermaid
sequenceDiagram
    autonumber
    participant Ops as Negocio (BD o backoffice)
    participant App
    participant Cust as ms-customer
    participant DB as customer_db

    Ops->>DB: activa una fila en experience_components<br/>(p. ej. campaign-black-friday)
    App->>Cust: GET /experience/home con If-None-Match
    Cust->>DB: cliente, preferencias y componentes activos del home
    Cust->>Cust: filtra por segmento, cliente, fechas,<br/>hora local y preferencia de promociones
    Cust->>Cust: ordena por posición y personaliza<br/>"{greeting}, {firstName}"
    alt el home no cambió
        Cust-->>App: 304 Not Modified (usa su caché)
    else cambió
        Cust-->>App: 200 lista de componentes y ETag nuevo
    end
    Note over App: dibuja cada type que conoce<br/>e ignora los desconocidos<br/>si falla, usa su caché o un layout de respaldo
```

## 5. Resiliencia: qué pasa cuando algo falla

```mermaid
flowchart LR
    subgraph falla["Falla"]
        f1["Servicio lento (menos de 10 s)"]
        f2["Servicio lento (más de 10 s)"]
        f3["Servicio caído"]
        f4["ms-auth caído"]
        f5["ms-customer caído"]
        f6["API de tipo de cambio caída"]
        f7["Base de datos saturada"]
    end

    subgraph backend["Respuesta del backend"]
        r1["Responde más tarde"]
        r2["Gateway corta: 503 service-unavailable"]
        r3["Gateway: 503 inmediato<br/>los demás servicios siguen"]
        r4["Login: 503<br/>sesiones abiertas siguen válidas (JWKS en caché)"]
        r5["Perfil y home: 503<br/>cuentas y transferencias siguen<br/>avisos push se pierden"]
        r6["Error solo en /external/fx"]
        r7["Timeout de conexión 3 s y de consulta 5 s:<br/>error rápido, sin hilos colgados"]
    end

    subgraph app["Lo que ve el cliente"]
        a1["Skeleton y datos en caché"]
        a2["Datos de hace X min<br/>y botón de reintento"]
        a3["Solo la sección afectada degrada"]
        a4["Puede seguir usando la app"]
        a5["Home de respaldo<br/>con sus cuentas visibles"]
        a6["Se oculta el bloque de tipo de cambio"]
        a7["Mensaje de error con correlationId"]
    end

    f1 --> r1 --> a1
    f2 --> r2 --> a2
    f3 --> r3 --> a3
    f4 --> r4 --> a4
    f5 --> r5 --> a5
    f6 --> r6 --> a6
    f7 --> r7 --> a7
```

Todos estos escenarios se pueden reproducir en vivo con los scripts de `chaos/` (ver el README).
