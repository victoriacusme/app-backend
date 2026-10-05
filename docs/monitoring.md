# Cómo monitorearía la aplicación en producción

La pregunta no es solo "¿está funcionando el servidor?", sino **"¿los clientes pueden hacer lo que vinieron a
hacer?"**. Un servidor puede responder sin errores y aun así el cliente estar frustrado. Por eso miro dos tipos de
problemas:

- **Operativos:** algo está caído, lento o saturado.
- **De experiencia:** todo "funciona", pero el cliente no logra iniciar sesión, transferir o registrarse.

## Las 4 preguntas que quiero poder responder

1. **¿Los clientes pueden hacer lo importante?** Iniciar sesión, ver su saldo y transferir. → Por esto me avisan
   las alertas.
2. **¿Dónde está el problema?** En qué servicio, en qué pantalla, en qué dependencia. → Lo veo en los tableros.
3. **¿Por qué pasó?** → Lo encuentro en los registros, siguiendo un identificador.
4. **¿A quién afecta?** A qué versión de la app, sistema operativo o tipo de cliente. → Lo veo con los datos de la
   app.

**Regla que sigo:** las alertas avisan de lo que **siente el cliente** (errores, lentitud, operaciones que no se
completan), no de causas técnicas como "el CPU está al 80 %". Las causas sirven para investigar, no para despertar
a alguien a las 3 de la mañana.

## Lo que ya dejé preparado

| Qué | Para qué sirve |
|---|---|
| **Métricas en cada servicio** (`/actuator/prometheus`) | Cuántas peticiones hay, cuántas fallan y cuánto tardan, por cada ruta. También el estado de la conexión a la base de datos y de la memoria |
| **Chequeo de salud** (`/actuator/health`) | Saber si cada servicio está vivo |
| **Identificador de seguimiento** (`X-Correlation-Id`) | Viaja por todos los servicios y aparece en cada registro y en cada error. Con él sigo el recorrido completo de una petición |
| **Errores con código fijo** (`insufficient-funds`, `service-unavailable`…) | Contar qué errores ven los clientes |
| **Registro del gateway** | Cada petición con su resultado y su tiempo, separando el tiempo del servicio del de la red |
| **Registros sin datos personales** | Se pueden enviar a una herramienta externa sin riesgo |

## Las herramientas que usaría

```
 📱 App ──────► Crashlytics o Sentry (cierres inesperados, lentitud, errores por pantalla)
 🌐 Gateway y servicios ──► registros ──► Loki / Elasticsearch / CloudWatch
 📊 Servicios ──► métricas ──► Prometheus + Grafana (o Datadog)
 🔍 Servicios ──► recorrido de cada petición ──► OpenTelemetry + Jaeger
 🤖 Prueba automática cada minuto ──► login → saldo → transferencia de $0,01
 🚨 Alertas ──► Slack (lo leve) / guardia de turno (lo grave)
```

Lo importante no es la marca de la herramienta, sino **qué miro y cuándo me aviso**.

## Mis metas de calidad (SLO)

Estas metas definen qué significa "funcionar bien" para el cliente:

| Acción del cliente | Meta |
|---|---|
| Iniciar sesión | Funciona el 99,9 % de las veces |
| Ver saldo y movimientos | El 95 % responde en menos de medio segundo |
| Transferir | Funciona el 99,9 % de las veces y el 95 % responde en menos de 1 segundo |
| Ver el home | Funciona el 99,5 % de las veces (la app tiene un home de respaldo) |

Un 99,9 % al mes permite unos **43 minutos de fallas al mes**. Si ese margen se está gastando demasiado rápido, me
avisa una alerta.

## Cómo detecto problemas operativos

Para cada servicio miro tres cosas: **cuántas peticiones llegan, cuántas fallan y cuánto tardan.**

| Señal | Qué me indica |
|---|---|
| Suben los errores 5xx | Algo se rompió: un despliegue reciente, una base de datos con problemas… |
| Sube el tiempo de respuesta | Algo se está saturando |
| **Bajan** de golpe las peticiones | Puede ser grave: los clientes no logran llegar al backend |
| Peticiones esperando conexión a la base de datos | La base está lenta o hay operaciones esperando su turno |
| Muchos errores "sesión inválida" tras un despliegue | Se cambiaron mal las claves de firma |
| Errores en `/external/fx` | Se cayó el proveedor de tipo de cambio (la app oculta solo ese bloque) |
| Avisos que no se pudieron enviar | Las notificaciones no están llegando |
| Servicios que se reinician solos | Falta de memoria o errores al arrancar |

Además, una **prueba automática** recorre cada minuto el camino crítico con un usuario de prueba: login → saldo →
transferencia de $0,01 entre dos cuentas de prueba. Así detecto lo que las métricas internas no ven, como un
certificado vencido o la entrada principal caída.

## Cómo detecto problemas de experiencia de usuario

### Contando lo que logran (y lo que no) los clientes

Agregaría contadores en cada punto importante:

| Qué cuento | Qué me revela |
|---|---|
| Logins correctos, fallidos y bloqueados | Un pico de bloqueos indica un ataque o una versión de la app con un error en el login |
| Registros: en qué paso se abandonan | Dónde está la fricción del registro |
| Transferencias completadas, rechazadas por saldo, rechazadas por cuenta ajena | El éxito real de la función más importante |
| **Veces que el cliente tocó "Verificar estado"** | Ver abajo 👇 |
| Notificaciones entregadas o fallidas | Si los avisos llegan |

**La señal más reveladora: "Verificar estado".** Cuando la app no recibe la respuesta de una transferencia (por un
corte de señal o por lentitud), **no reintenta sola**: le muestra al cliente "No pudimos confirmar el resultado" y un
botón "Verificar estado", que reenvía la misma solicitud con el mismo `Idempotency-Key`. Si ese botón se usa cada vez
más, los clientes están viviendo una mala experiencia, **aunque el backend nunca haya fallado**. Lo detecto porque
esas respuestas llevan la marca `Idempotent-Replayed: true`.

Otras pistas:
- **Muchos rechazos por saldo insuficiente:** la app podría estar mostrando un saldo desactualizado.
- **Conflictos de "número de recibo"** (error 409): casi siempre es un error de la app.

### Desde la propia app

La app es la única que ve lo que vive el cliente. Desde ahí mediría:

| Qué | Para qué |
|---|---|
| Cierres inesperados de la app, por versión | Detener la publicación de una versión defectuosa |
| Cuánto tarda en abrir y en cargar cada pantalla | Lentitud percibida, aunque el servidor responda rápido |
| Errores de red, por tipo de conexión (wifi o datos móviles) | Problemas que el servidor nunca ve |
| Cuántas veces se muestra "Sin conexión" o "Datos de hace X minutos" | Cuánto tiempo los clientes ven información vieja |
| Bloques del home que no se pudieron mostrar | Una campaña mal configurada |

Siempre **sin datos personales**: solo versión, sistema operativo y tipo de cliente.

### Soporte al cliente

Las pantallas de error muestran el identificador de seguimiento. Si un cliente llama, soporte lo busca y ve todo el
recorrido de esa petición, **sin pedirle datos personales**.

## Alertas

| Alerta | Cuándo | Gravedad | Qué hago primero |
|---|---|---|---|
| Errores en un servicio | Más del 2 % de las peticiones fallan durante 5 min | 🔴 Crítica | Revisar el tablero del servicio y si hubo un despliegue reciente |
| Lentitud | El 95 % tarda más de 1 s durante 10 min | 🟠 Alta | Ver si la demora está en el servicio o en la base de datos |
| La prueba automática falla | 2 veces seguidas | 🔴 Crítica | Verificar desde afuera: dominio, certificado, entrada |
| Un servicio no responde | Su chequeo de salud falla | 🔴 Crítica | Revisar los registros de arranque |
| Base de datos saturada | Peticiones esperando conexión durante 5 min | 🟠 Alta | Buscar consultas lentas u operaciones trabadas |
| Muchos usuarios bloqueados | 5 veces más de lo normal | 🟡 Media | ¿Ataque o error en el login de la app? |
| "Verificar estado" frecuente | Más del 5 % de las transferencias | 🟡 Media | Revisar la lentitud entre la app y el servidor |
| La app se cierra sola | Menos del 99,5 % de usuarios sin cierres en la última versión | 🟠 Alta | Detener la publicación de esa versión |
| Falla el tipo de cambio | Más de la mitad de las consultas durante 15 min | 🟢 Baja | Avisar al proveedor; la app ya se adapta sola |

Las alertas leves van a un canal de Slack. Solo las críticas despiertan a la persona de guardia.

## Tableros

1. **Experiencia del cliente:** metas de calidad, logins, transferencias, registro paso a paso, uso de "Verificar
   estado" y cierres de la app.
2. **Servicios:** peticiones, errores y tiempos de cada ruta, marcando cada despliegue.
3. **Infraestructura:** bases de datos, memoria, gateway y servicios externos.

## Un ejemplo de punta a punta

1. **Me llega la alerta:** "más del 2 % de errores en `ms-accounts`".
2. **Miro el tablero:** solo falla "transferir", tarda más de lo normal y hay peticiones esperando a la base de
   datos.
3. **Busco en los registros** los errores recientes y tomo un identificador de seguimiento.
4. **Sigo ese identificador** y veo que el tiempo se pierde esperando que se libere una cuenta.
5. **Encuentro la causa:** un proceso nocturno estaba reteniendo cuentas. Lo corrijo y el tablero muestra la
   recuperación.
6. **Mientras tanto, el cliente:** vio "No pudimos confirmar el resultado", sin que la app reintentara por su
   cuenta. Al tocar "Verificar estado", si la transferencia ya se había hecho, recibió la original. **Nunca se le
   cobró dos veces.**

## Lo que falta para tenerlo funcionando

| Tarea | Esfuerzo |
|---|---|
| Medir los tiempos en percentiles (para la meta del 95 %) | Una línea de configuración por servicio |
| Contadores de logins, registros, transferencias y notificaciones | Pocas líneas por caso de uso |
| Registros en formato JSON | Configuración de los servicios y del gateway |
| Seguimiento de cada petición entre servicios (OpenTelemetry) | Una variable de entorno por servicio |
| Prometheus y Grafana con estos tableros y alertas | Fase 8 del plan de monitoreo, cuando se realice |
| Mediciones dentro de la app | Lado del front |
