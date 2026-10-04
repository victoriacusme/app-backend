# Nexo Bank · Backend

[![Backend CI](https://github.com/victoriacusme/app-backend/actions/workflows/backend.yml/badge.svg)](https://github.com/victoriacusme/app-backend/actions/workflows/backend.yml)

Backend de **Nexo Bank** (banca personas) para la app móvil en Flutter. Son tres microservicios Spring Boot con
arquitectura hexagonal y una base de datos por servicio, detrás de un gateway Nginx.

```
                         App Flutter
                              │  HTTPS · JWT · JWE (login) · Idempotency-Key · X-Correlation-Id
                    ┌─────────▼──────────┐
                    │  Gateway (Nginx)   │  :8080 · ruteo · rate limit · bloquea /internal y /actuator
                    └─────────┬──────────┘
                    ┌─────────▼──────────┐
                    │     Toxiproxy      │  simula latencia y caídas (chaos/)
                    └──┬───────┬───────┬─┘
          ┌────────────▼┐ ┌────▼──────────┐ ┌▼─────────────┐      open.er-api.com
          │   ms-auth   │ │  ms-customer  │ │ ms-accounts  │      (tipo de cambio,
          │ login · JWT │ │ perfil · SDUI │ │ cuentas      │       vía /external/fx)
          │ refresh     │ │ preferencias  │ │ movimientos  │
          │ onboarding  │ │ push          │ │ transferencias│
          └──────┬──────┘ └──────┬────────┘ └──────┬───────┘
              auth_db       customer_db        accounts_db        (PostgreSQL 16)
```

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| `gateway` | 8080 | Única entrada para la app |
| `ms-auth` | 8081 | Login (JSON o JWE), JWT RS256, refresh rotativo, logout, JWKS, onboarding |
| `ms-customer` | 8082 | Perfil, preferencias, experiencia dinámica (SDUI) y notificaciones push |
| `ms-accounts` | 8083 | Cuentas, movimientos y transferencias entre cuentas propias |
| `toxiproxy` | 8474 | API de control para los scripts de caos |
| `auth-db` · `customer-db` · `accounts-db` | 5433 · 5434 · 5435 | Una BD PostgreSQL por servicio |

## Cómo levantarlo

**Requisitos:** Docker con Compose v2. Para ejecutar los tests fuera de Docker: Java 21 y Maven 3.9.

```bash
docker compose up --build -d     # la primera vez tarda unos minutos (compila los tres servicios)
docker compose ps                # los servicios quedan "healthy" en ~30 s
curl localhost:8080/health       # {"status":"UP"}
```

Las bases de datos se crean con Flyway al arrancar e incluyen **datos de prueba**: clientes, cuentas con saldos
coherentes, movimientos y experiencias por segmento.

```bash
docker compose logs -f ms-accounts   # logs de un servicio
docker compose down                  # apagar (los datos se conservan)
docker compose down -v               # apagar y borrar las bases de datos
```

> **Configuración opcional:** copia `.env.example` a `.env` para fijar tus propias claves. Sin `.env` se usan
> valores de desarrollo. Las claves JWT, si no se fijan, se regeneran en cada reinicio de ms-auth y las sesiones
> abiertas dejan de servir.

## Usuarios de prueba

Contraseña de los tres: **`Nexo2026*`**

| Usuario | Segmento | Qué ve en el home |
|---|---|---|
| `ana` | Joven | Recargas, meta de ahorro (viaje), promoción de metas |
| `carlos` | Premium | Inversiones, asesor, **tipo de cambio** |
| `lucia` | Emprendedor | Cobro con QR, pago a proveedores, crédito para el negocio |

También se pueden crear usuarios nuevos con `POST /auth/register` (onboarding completo: credenciales, cliente y
cuenta de ahorros).

## API (vía gateway, `http://localhost:8080`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` | Login. JSON o JWE (`Content-Type: application/jose`) |
| POST | `/auth/refresh` · `/auth/logout` | Rotación y revocación del refresh token |
| POST | `/auth/register` | Onboarding: crea credenciales, cliente y cuenta (con compensación si algo falla) |
| GET | `/.well-known/jwks.json` | Claves públicas: firma de JWT (`sig`) y cifrado del login (`enc`) |
| GET | `/accounts` · `/accounts/{id}` | Cuentas del cliente (número enmascarado, montos como texto) |
| GET | `/accounts/{id}/movements?size=20&cursor=` | Movimientos paginados por cursor |
| POST | `/transfers/own` | Transferencia entre cuentas propias. Header **`Idempotency-Key`** obligatorio |
| GET | `/transfers/{id}` | Estado de una transferencia |
| GET | `/customers/me` | Perfil (cédula y teléfono enmascarados) y preferencias |
| PATCH | `/customers/me/preferences` | Idioma, tema, notificaciones y promociones (parcial) |
| PUT · DELETE | `/customers/me/devices` · `/customers/me/devices/{token}` | Registro del token de push del teléfono |
| GET | `/experience/home` | Home server-driven por segmento, hora y preferencias (con ETag) |
| GET | `/external/fx/latest/{moneda}` | Tipo de cambio (servicio externo `open.er-api.com`) |

**Convenciones:**
- Todas las rutas salvo login, refresh, register, JWKS y tipo de cambio requieren `Authorization: Bearer <accessToken>`.
- El cliente siempre se toma del token, nunca de un parámetro.
- Los montos viajan como texto (`"125.50"`) y las fechas en ISO-8601 UTC.
- Los errores son `ProblemDetail` con un `code` estable (`insufficient-funds`, `account-not-owned`...) y un
  `correlationId`.
- Toda respuesta lleva `X-Correlation-Id`. Si la app lo envía, se respeta y viaja por todos los servicios.

### Ejemplo rápido

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"ana","password":"Nexo2026*"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])')

curl -s localhost:8080/accounts -H "Authorization: Bearer $TOKEN"
curl -s localhost:8080/experience/home -H "Authorization: Bearer $TOKEN"

curl -s -X POST localhost:8080/transfers/own -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" -H 'Content-Type: application/json' \
  -d '{"sourceAccountId":"c0000000-0000-0000-0000-000000000011",
       "targetAccountId":"c0000000-0000-0000-0000-000000000012","amount":"25.00","description":"Ahorro"}'
```

## Demos

### Resiliencia: latencia y caídas (Toxiproxy)

Toxiproxy se ubica entre el gateway y cada servicio, así que permite degradar uno solo:

```bash
chaos/latency.sh accounts 3000   # +3 s en ms-accounts (la app muestra skeleton o datos en caché)
chaos/latency.sh accounts 12000  # supera el timeout del gateway (10 s): 503 service-unavailable
chaos/down.sh accounts           # ms-accounts caído: 503 inmediato; login y perfil siguen funcionando
chaos/status.sh                  # estado de cada servicio
chaos/reset.sh                   # todo vuelve a la normalidad
```

### Experiencia dinámica (SDUI): cambiar la app sin publicarla

```bash
scripts/experience.sh list                        # componentes del home y su estado
scripts/experience.sh on campaign-black-friday    # aparece un banner para todos los clientes
scripts/experience.sh off campaign-black-friday
```

### Notificaciones push

La app registra su token con `PUT /customers/me/devices`. Al completarse una transferencia, ms-accounts avisa a
ms-customer, que arma el texto en el idioma del cliente y lo envía a sus dispositivos.

- **Sin credenciales de Firebase** (por defecto), la notificación se registra en el log:
  `docker compose logs -f ms-customer | grep "Push simulado"`.
- **Para enviar por FCM**, monta la cuenta de servicio de Firebase (JSON) en el contenedor y define
  `FCM_CREDENTIALS_FILE` con su ruta.

## Tests

```bash
mvn test      # unitarios (Mockito) y de controladores; no necesita Docker
mvn verify    # además, integración con PostgreSQL real (Testcontainers); necesita Docker
```

El CI (GitHub Actions, `.github/workflows/backend.yml`) ejecuta `mvn verify` en cada push y en cada PR, y valida el
`docker-compose.yml`, la configuración del gateway y los scripts.

Los tests de integración cubren, entre otros casos:
- Concurrencia de transferencias: no se gasta más del saldo, no hay deadlocks y un doble toque genera una sola
  transferencia.
- Coherencia de la semilla.
- Paginación sin duplicados.
- Cifrado en reposo.
- Experiencias por segmento.

## Configuración

Variables principales (ver `.env.example`):

| Variable | Uso | Por defecto (desarrollo) |
|---|---|---|
| `JWT_SIGNING_KEY` / `JWT_ENCRYPTION_KEY` | Claves RSA (PEM PKCS#8) de ms-auth | Efímeras: se generan al arrancar |
| `INTERNAL_API_KEY` | API key de los endpoints `/internal/**` entre servicios | `dev-internal-key-change-me` |
| `CUSTOMER_DATA_KEY` | Clave AES-256 para cifrar cédula y teléfono | Clave fija de desarrollo en el compose |
| `FCM_CREDENTIALS_FILE` | Credenciales de Firebase para push real | Vacío: push simulado en el log |
| `DB_USER` / `DB_PASSWORD` | Credenciales de las BD | `nexo` / `nexo` |

## Estructura

```
├── ms-auth/ · ms-customer/ · ms-accounts/   microservicios (Spring Boot 3.5, Java 21)
│   └── src/main/java/ec/nexo/<servicio>/
│       ├── domain/           modelo y reglas de negocio (sin dependencias de frameworks)
│       ├── application/      casos de uso y puertos de salida
│       └── infrastructure/   adaptadores: web, JPA, HTTP, push, configuración
├── gateway/nginx.conf       gateway
├── chaos/                   Toxiproxy y scripts de caos
├── scripts/                 utilidades de demo (experiencia dinámica)
├── docs/                    documentación técnica
└── docker-compose.yml
```

## Documentación

- [`docs/security.md`](docs/security.md): autenticación, JWE, cifrado en reposo, servicio a servicio (mTLS), logs
  sin datos personales y gestión de secretos.
- [`docs/monitoring.md`](docs/monitoring.md): monitoreo en producción, SLO, alertas y cómo detectar problemas
  operativos y de experiencia de usuario.

## Solución de problemas

- **`docker compose up` falla al descargar imágenes (timeout):** algunas redes bloquean el CDN de Docker Hub.
  Descarga las imágenes desde el espejo de Google y vuelve a intentarlo:
  ```bash
  for img in postgres:16-alpine eclipse-temurin:21-jre-alpine maven:3.9-eclipse-temurin-21-alpine nginx:1.27-alpine; do
    docker pull mirror.gcr.io/library/$img && docker tag mirror.gcr.io/library/$img $img
  done
  ```
- **`docker compose ps` no muestra nada:** revisa `docker context ls`. Si Docker Desktop está activo, vuelve al
  Docker del sistema con `docker context use default`.
- **Las sesiones dejan de servir tras reiniciar ms-auth:** fija `JWT_SIGNING_KEY` y `JWT_ENCRYPTION_KEY` en `.env`.
