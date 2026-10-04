# Monitoreo en producción

Cómo se monitorearía Nexo Bank en producción y cómo se detectarían los problemas **operativos** (algo está caído,
lento o saturado) y los de **experiencia de usuario** (todo "funciona", pero el cliente no logra lo que quiere).

## Enfoque

El monitoreo tiene que responder, en este orden, cuatro preguntas:

1. **¿Los clientes pueden hacer lo importante?** Iniciar sesión, ver sus saldos y transferir. Es lo que se alerta.
2. **¿Dónde está el problema?** En qué servicio, endpoint o dependencia. Lo responden los dashboards.
3. **¿Por qué pasó?** Lo responden los logs y las trazas, unidos por el correlation-id.
4. **¿A quién afecta?** Segmento, versión de la app, plataforma o tipo de red. Lo responden los datos del cliente.

Se alerta por **síntomas** que siente el usuario (errores, latencia, operaciones que no se completan) y no por causas
(CPU al 80 %). Las causas se usan para diagnosticar, no para despertar a nadie.

## Qué ya está preparado en el backend

| Señal | Estado | Detalle |
|---|---|---|
| Métricas de cada servicio | ✅ | Actuator + Micrometer en `/actuator/prometheus`: peticiones HTTP por `uri`, `status` y `outcome`, pool de conexiones (`hikaricp_*`), JVM, GC y CPU |
| Health checks | ✅ | `/actuator/health` en los tres servicios y `/health` en el gateway; Docker los usa para el estado `healthy` |
| Correlation-id | ✅ | `X-Correlation-Id` viaja app → gateway → servicios → llamadas internas; aparece en cada línea de log y en el campo `correlationId` de los errores |
| Errores clasificables | ✅ | Todo error es `ProblemDetail` con un `code` estable (`insufficient-funds`, `service-unavailable`, `user-locked`...) |
| Log de acceso del gateway | ✅ | Cada petición con estado, tiempo total, tiempo del servicio (`upstream_response_time`) y correlation-id |
| Logs sin datos personales | ✅ | Ver `docs/security.md`; se pueden enviar a una plataforma externa sin riesgo |
| Histogramas de latencia | ⬜ | Necesarios para p95/p99. Una línea: `management.metrics.distribution.percentiles-histogram.http.server.requests=true` |
| Métricas de negocio | ⬜ | Contadores de transferencias, logins, onboarding y push (ver más abajo) |
| Trazas distribuidas | ⬜ | OpenTelemetry (agente Java), sin cambios de código |

## Arquitectura de observabilidad propuesta

```
 App Flutter ──► Crashlytics / Sentry (crashes, ANR, rendimiento, errores de red por pantalla)
     │
     ▼
 Gateway ─────► log de acceso (JSON) ─────────────┐
     │                                            ▼
 ms-auth · ms-customer · ms-accounts ──► logs ──► Loki / Elasticsearch / CloudWatch Logs
     │   └──── /actuator/prometheus ──► Prometheus (o Datadog / CloudWatch / Grafana Cloud)
     │   └──── agente OpenTelemetry ──► Tempo / Jaeger (trazas)
     ▼
 PostgreSQL ──► métricas de la BD gestionada (conexiones, locks, réplica, disco)
                                                  │
                     Grafana (dashboards) ◄───────┘──► Alertmanager ──► Slack / PagerDuty
 Sondas sintéticas ──► (login, cuentas, transferencia de prueba cada minuto)
```

Con un proveedor gestionado (Datadog, New Relic, Grafana Cloud) la idea es la misma: lo importante son las
señales y las alertas, no la herramienta.

## Objetivos de servicio (SLO)

Los SLO definen qué es "funcionar bien" desde el punto de vista del cliente. Son la base de las alertas.

| Recorrido | Indicador (SLI) | Objetivo |
|---|---|---|
| Iniciar sesión | Logins que no terminan en 5xx | 99,9 % mensual |
| Ver cuentas y movimientos | Peticiones respondidas en < 500 ms | 95 % |
| Transferir | Transferencias sin error técnico (5xx o timeout) | 99,9 % |
| Transferir | Tiempo de respuesta | p95 < 1 s |
| Home personalizado | `/experience/home` sin 5xx | 99,5 % (la app tiene un layout de respaldo) |

Un 99,9 % mensual deja unos **43 minutos de presupuesto de error**. Las alertas de SLO se disparan cuando ese
presupuesto se consume demasiado rápido (*burn rate*): por ejemplo, el 2 % del presupuesto mensual en una hora.

## Detectar problemas operativos

Para cada servicio se siguen las señales **RED** (peticiones, errores y duración) y, para sus dependencias, la
**saturación**.

### Servicios

```promql
# Tasa de errores 5xx por servicio
sum by (job) (rate(http_server_requests_seconds_count{outcome="SERVER_ERROR"}[5m]))
  / sum by (job) (rate(http_server_requests_seconds_count[5m]))

# Latencia p95 por endpoint (requiere activar los histogramas)
histogram_quantile(0.95, sum by (le, job, uri) (rate(http_server_requests_seconds_bucket[5m])))

# Tráfico: una caída brusca también es un síntoma (la app no logra llegar al backend)
sum by (job) (rate(http_server_requests_seconds_count[5m]))
```

Las métricas llevan el `uri` como plantilla (`/accounts/{accountId}`), así que no explotan en cardinalidad.

### Dependencias y saturación

| Qué | Métrica o señal | Qué indica |
|---|---|---|
| Pool de conexiones a la BD | `hikaricp_connections_pending > 0` sostenido, `hikaricp_connections_timeout_total` creciendo | BD lenta, locks o pool chico. Con el timeout de 3 s configurado, el cliente ve 5xx rápido en vez de quedarse colgado |
| Locks de transferencias | Duración de `POST /transfers/own`; en la BD, `pg_locks` y las consultas lentas | Contención sobre una misma cuenta |
| JWKS de ms-auth | Errores 401 masivos en accounts o customer tras un despliegue de ms-auth | Rotación de claves mal hecha (en caché se tolera una caída breve) |
| Servicio externo de tipo de cambio | Tasa de 5xx/504 en `/external/fx` (log del gateway) | Proveedor caído; la app oculta solo ese bloque |
| Notificaciones push | Logs `No se pudo notificar` (ms-accounts) y entregas fallidas de FCM | Avisos que no llegan; la transferencia igual se completa |
| JVM y contenedores | Memoria, pausas de GC, reinicios del contenedor, CPU | Causas de lentitud o caídas |
| Gateway | 502/503/504 y 429 por ruta; `upstream_response_time` frente a `request_time` | Si la demora está en el servicio o en la red |

### Sondas sintéticas

Un proceso externo ejecuta cada minuto, con un usuario de prueba, el recorrido crítico: **login → cuentas → home →
transferencia de $0,01 entre dos cuentas propias de prueba** (con `Idempotency-Key`). Detecta lo que las métricas
internas no ven, como un DNS mal configurado, un certificado vencido o el gateway caído. Además mide la latencia
desde fuera de la plataforma.

## Detectar problemas de experiencia de usuario

Un backend sin errores 5xx no garantiza una buena experiencia. Estas señales detectan cuando el cliente
**no logra su objetivo**, aunque técnicamente todo responda.

### Métricas de negocio (embudos)

Se agregan con contadores de Micrometer, uno por punto de decisión (unas pocas líneas por caso de uso):

| Métrica | Etiquetas | Qué revela |
|---|---|---|
| `nexo_logins_total` | `result`: ok, invalid_credentials, locked | Un pico de `locked` indica un ataque o un cambio en la app que rompe el login |
| `nexo_onboarding_total` | `step`, `result` | Dónde se cae el registro; `onboarding-unavailable` indica un servicio interno caído |
| `nexo_transfers_total` | `result`: completed, insufficient_funds, not_owned, replayed, conflict | El éxito real de las transferencias |
| `nexo_push_total` | `result`: delivered, invalid_token, failed, skipped | Si los avisos llegan |

**Señales clave:**
- **Reintentos (`replayed`):** cada respuesta con `Idempotent-Replayed: true` significa que la app **no recibió** la
  respuesta original (timeout o corte de red) y tuvo que reintentar. Que crezcan es un síntoma directo de mala
  experiencia, aunque el backend nunca haya fallado.
- **`insufficient_funds` inusualmente alto:** la app podría estar mostrando un saldo desactualizado (caché vieja).
- **`conflict` (409, Idempotency-Key reutilizada):** casi siempre es un bug del cliente al generar las claves.
- **Tasa de conversión del onboarding** por paso y por versión de la app.

### Desde la app

La app es la única que ve lo que vive el cliente, por eso se instrumenta (Crashlytics o Sentry):

| Señal | Para qué |
|---|---|
| Usuarios sin crashes (*crash-free users*) y ANR por versión | Detectar un release malo y frenar su despliegue gradual |
| Tiempo de arranque y de carga de cada pantalla | Lentitud percibida, aunque el backend responda rápido |
| Latencia y errores de red vistos desde el dispositivo, por tipo de red | Problemas en redes móviles que el servidor no ve |
| Veces que se muestra "Sin conexión", "Datos de hace X min" o se usa el layout de respaldo del home | Cuánto tiempo los clientes ven datos viejos o degradados |
| Aperturas del circuit breaker y reintentos | Servicios inestables desde la óptica del cliente |
| Componentes SDUI desconocidos o con error de render | Una experiencia publicada en BD que la app no sabe dibujar |
| Errores por `code` y por pantalla | Qué errores de negocio ven los clientes y dónde |
| Embudos: login → home → transferencia → comprobante; pasos del onboarding | Abandono y fricción |

Todos estos eventos llevan la versión de la app, la plataforma y el segmento, **nunca datos personales**.

### Soporte al cliente

Las pantallas de error muestran el `correlationId`. Si un cliente llama, soporte busca ese id y ve el recorrido
completo (gateway → servicio → llamadas internas) **sin pedirle datos personales**. Con trazas distribuidas, el
mismo id lleva a la traza con el tiempo de cada tramo.

## Alertas

| Alerta | Condición | Severidad | Primera acción |
|---|---|---|---|
| Errores en un servicio | 5xx > 2 % durante 5 min | Crítica | Dashboard del servicio → logs por `code` y correlation-id → ¿hubo un despliegue? |
| Latencia | p95 > 1 s durante 10 min (p95 > 1,5 s en transferencias) | Alta | Separar `upstream_response_time` de la red; revisar el pool de BD y los locks |
| SLO de transferencias | Burn rate rápido del presupuesto de error | Crítica | Igual que errores; considerar rollback |
| Sonda sintética | 2 fallos seguidos del recorrido crítico | Crítica | Verificar desde fuera: DNS, certificado, gateway |
| Servicio caído | Health check fallando o reinicios repetidos del contenedor | Crítica | Logs del arranque (migraciones de Flyway, claves faltantes) |
| Pool de BD saturado | `hikaricp_connections_pending > 0` durante 5 min | Alta | Consultas lentas y locks en PostgreSQL |
| Bloqueos de usuarios | `user-locked` x5 sobre la línea base | Media | ¿Ataque de fuerza bruta (por IP) o bug del login en la app? |
| Reintentos idempotentes | `replayed` > 5 % de las transferencias | Media | Timeouts entre la app y el gateway; latencia en redes móviles |
| Crash rate de la app | Usuarios sin crashes < 99,5 % en la última versión | Alta | Detener el despliegue gradual de esa versión |
| Servicio externo | Errores en `/external/fx` > 50 % durante 15 min | Baja | Avisar; la app degrada sola (oculta el bloque) |
| Push | Entregas fallidas > 10 % durante 30 min | Baja | Credenciales de FCM o cuota |

Cada alerta enlaza a un **runbook** corto (qué mirar y qué hacer) y a su dashboard. Las de severidad baja van a un
canal de Slack, no a la guardia.

## Dashboards

1. **Experiencia y negocio:** SLO y presupuesto de error, logins, transferencias por resultado, embudo del
   onboarding, reintentos, crash-free users y uso de modos degradados en la app.
2. **Servicios (RED):** por servicio y endpoint, tráfico, errores por `code` y latencia p50/p95/p99, con las marcas de
   cada despliegue.
3. **Dependencias e infraestructura:** pools de BD, PostgreSQL (conexiones, locks, consultas lentas), JVM, contenedores,
   gateway (5xx, 429, latencia upstream), servicio externo y FCM.

## Un incidente, de punta a punta

1. **Alerta:** "5xx en ms-accounts > 2 %".
2. **Dashboard:** solo falla `POST /transfers/own`; la latencia subió y el pool de BD tiene conexiones en espera.
3. **Logs:** filtrando `code=internal-error` aparece un timeout de consulta. Se toma un `correlationId`.
4. **Traza:** ese id muestra que el tiempo se va en el `SELECT ... FOR UPDATE`. Hay contención de locks.
5. **Causa:** un job batch está bloqueando cuentas. Se corrige y el dashboard confirma la recuperación.
6. **Mientras tanto, la experiencia:** la app mostró "No pudimos completar la transferencia". Gracias a la
   idempotencia, los reintentos no duplicaron dinero.

## Qué falta para tenerlo funcionando

| Tarea | Esfuerzo |
|---|---|
| Activar histogramas de latencia (`percentiles-histogram`) | Una línea de configuración por servicio |
| Contadores de negocio (logins, onboarding, transferencias, push) | Pocas líneas por caso de uso |
| Logs en JSON (gateway y servicios) para indexarlos | Configuración de Logback y de Nginx |
| Agente de OpenTelemetry para trazas | Variable de entorno en cada contenedor |
| Prometheus y Grafana con los dashboards y las alertas de este documento | Fase 8 del plan |
| Instrumentación de la app (Crashlytics o Sentry, eventos de embudo y modos degradados) | Lado del front |
