# Cómo está armado el backend

Este documento muestra, con diagramas, cómo están conectadas las piezas y cómo funcionan los flujos más
importantes: iniciar sesión, transferir, registrarse y armar el home. Los diagramas se leen de arriba hacia abajo.

> Las razones detrás de cada decisión están en [`decisions.md`](decisions.md).

## 1. Las piezas y cómo se conectan

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

**Cómo leerlo:**
- **La app habla con una sola puerta (el gateway).** El gateway reparte cada petición al servicio que
  corresponde.
- **Toxiproxy** está en el medio solo para simular fallas en las demos. En producción no existe.
- **Tres servicios, cada uno con su propia base de datos.** Ninguno lee la base de datos de otro.
- **Las líneas punteadas** son conversaciones internas entre servicios, protegidas con una clave. La app no puede
  llamarlas.
- **Para verificar la sesión**, `ms-customer` y `ms-accounts` no le preguntan a `ms-auth` en cada petición: usan la
  clave pública que guardaron en memoria.

## 2. Cómo está organizado cada servicio por dentro

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

**La idea:** las reglas del banco (el dominio) están en el centro y no dependen de nada técnico. Alrededor están
las piezas que se pueden cambiar: la base de datos, las llamadas a otros servicios, las notificaciones. Gracias a
eso, las reglas se prueban en milisegundos y sin base de datos.

## 3. Qué se guarda en cada base de datos

Cada servicio tiene su base. El identificador del cliente (`customer_id`) se repite entre ellas para relacionarlas,
pero ninguna se conecta directamente con otra.

**Usuarios y sesiones** (`ms-auth`): la contraseña nunca se guarda, solo una huella irreversible (Argon2).

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

**Clientes, preferencias y home** (`ms-customer`): la cédula y el teléfono se guardan cifrados.

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

**Cuentas y dinero** (`ms-accounts`): cada transferencia genera dos movimientos, uno de salida y uno de entrada.

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

## 4. Los flujos más importantes

### 4.1 Iniciar sesión y mantener la sesión abierta

**En resumen:** la app cifra la contraseña antes de enviarla. Si es correcta, recibe un pase de 15 minutos y un
pase de renovación. Si alguien intenta usar un pase de renovación que ya se usó, se cierran todas las sesiones de
ese usuario, por si fue robado.

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

### 4.2 Transferir entre cuentas propias

**En resumen:** cada intento lleva un "número de recibo" único. El backend reserva las dos cuentas, valida todo y
mueve el dinero en un solo paso. Si el mismo recibo llega dos veces, devuelve la transferencia original **sin mover
el dinero de nuevo**. El aviso al celular sale después, sin hacer esperar al cliente.

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

**Si la app no recibe respuesta** (por ejemplo, se cae la señal), **no reintenta sola**: le muestra al cliente "No
pudimos confirmar el resultado" y un botón **"Verificar estado"**, que reenvía el mismo recibo. Si la transferencia
ya se había hecho, el cliente ve la original; el dinero nunca se mueve dos veces.

### 4.3 Registrarse (con marcha atrás si algo falla)

**En resumen:** el registro crea el cliente y su cuenta, y **al final** el usuario. Si la cuenta no se puede abrir,
se borra el cliente que alcanzó a crearse y se pide reintentar. Así nunca queda alguien que pueda iniciar sesión sin
tener cuenta.

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

### 4.4 Armar el home de cada cliente

**En resumen:** el home es una lista de bloques guardada en la base de datos. El backend elige los que le
corresponden a cada cliente (según su segmento, la hora y sus preferencias) y la app los dibuja. Si el negocio
activa una campaña en la base de datos, aparece sin publicar una versión nueva de la app.

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

## 5. Qué pasa cuando algo falla

Cada falla afecta solo a su parte; el resto de la app sigue funcionando.

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

Todos estos casos se pueden mostrar en vivo con los scripts de `chaos/` (ver el README).
