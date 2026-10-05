# Decisiones de arquitectura (ADR)

Cada decisión sigue el mismo formato: **contexto**, **decisión**, **alternativas** que se evaluaron y
**consecuencias** (lo que se gana y lo que se paga).

| # | Decisión | Estado |
|---|---|---|
| 1 | [Arquitectura hexagonal en cada microservicio](#adr-1-arquitectura-hexagonal-en-cada-microservicio) | Aceptada |
| 2 | [Tres microservicios con una base de datos cada uno](#adr-2-tres-microservicios-con-una-base-de-datos-cada-uno) | Aceptada |
| 3 | [JWT RS256 validado con JWKS](#adr-3-jwt-rs256-validado-con-jwks) | Aceptada |
| 4 | [Refresh token opaco, rotativo y con detección de reutilización](#adr-4-refresh-token-opaco-rotativo-y-con-detección-de-reutilización) | Aceptada |
| 5 | [Login cifrado con JWE](#adr-5-login-cifrado-con-jwe) | Aceptada |
| 6 | [Transferencias dentro de ms-accounts, en una sola transacción](#adr-6-transferencias-dentro-de-ms-accounts-en-una-sola-transacción) | Aceptada |
| 7 | [Idempotencia con `Idempotency-Key` y bloqueo pesimista ordenado](#adr-7-idempotencia-con-idempotency-key-y-bloqueo-pesimista-ordenado) | Aceptada |
| 8 | [Paginación de movimientos por cursor](#adr-8-paginación-de-movimientos-por-cursor) | Aceptada |
| 9 | [Experiencia dinámica (SDUI) guardada en base de datos](#adr-9-experiencia-dinámica-sdui-guardada-en-base-de-datos) | Aceptada |
| 10 | [Nginx como gateway](#adr-10-nginx-como-gateway) | Aceptada |
| 11 | [Onboarding síncrono con compensación](#adr-11-onboarding-síncrono-con-compensación) | Aceptada |
| 12 | [Cifrado en reposo en la aplicación (AES-256-GCM)](#adr-12-cifrado-en-reposo-en-la-aplicación-aes-256-gcm) | Aceptada |
| 13 | [Notificaciones: ms-customer es el dueño y el aviso es asíncrono tras el commit](#adr-13-notificaciones-ms-customer-es-el-dueño-y-el-aviso-es-asíncrono-tras-el-commit) | Aceptada |
| 14 | [Toxiproxy para demostrar la resiliencia](#adr-14-toxiproxy-para-demostrar-la-resiliencia) | Aceptada |
| 15 | [Contrato de API: montos como texto y errores `ProblemDetail`](#adr-15-contrato-de-api-montos-como-texto-y-errores-problemdetail) | Aceptada |

---

## ADR-1: Arquitectura hexagonal en cada microservicio

**Contexto.** Las reglas de negocio de un banco (bloqueo por intentos, saldo, propiedad de las cuentas,
idempotencia) deben poder probarse sin base de datos ni HTTP, y sobrevivir a cambios de tecnología.

**Decisión.** Cada servicio tiene tres capas:
- `domain`: modelo y reglas, sin dependencias de Spring.
- `application`: casos de uso y puertos de salida.
- `infrastructure`: adaptadores web, JPA, HTTP, push y configuración.

Los casos de uso se ensamblan en `UseCaseConfig` y no llevan anotaciones de componente. La única concesión es
`@Transactional` en los casos de uso.

**Alternativas.**
- *Capas clásicas (controller → service → repository):* menos archivos, pero la lógica termina acoplada a JPA y a
  Spring.
- *Clean architecture estricta:* sin anotaciones de Spring en `application`; la transacción se maneja con un
  decorador. Es más pura, pero tiene más código ceremonial.

**Consecuencias.**
- ✅ Los casos de uso se prueban con Mockito en milisegundos.
- ✅ Los adaptadores se prueban aparte, contra PostgreSQL real.
- ✅ Cambiar un adaptador (por ejemplo, de push simulado a FCM) no toca el dominio.
- ❌ Más clases (entidad JPA, modelo de dominio y mapeos) y algo de código repetido entre servicios.

## ADR-2: Tres microservicios con una base de datos cada uno

**Contexto.** La prueba pide microservicios. Los dominios son claros: identidad, cliente y cuentas.

**Decisión.**
- `ms-auth`: credenciales y sesiones.
- `ms-customer`: perfil, preferencias, experiencia y notificaciones.
- `ms-accounts`: cuentas, movimientos y transferencias.

Cada uno tiene **su propia base de datos PostgreSQL** y sus migraciones Flyway. Se relacionan solo por el
`customerId`, que viaja como `sub` en el JWT.

**Alternativas.**
- *Monolito modular:* más simple de operar y con transacciones locales entre módulos. Era una opción razonable para
  este tamaño, pero no cumplía el requisito.
- *Base de datos compartida:* facilita los joins, pero acopla los servicios por el esquema y anula su autonomía de
  despliegue.

**Consecuencias.**
- ✅ Cada servicio escala, se despliega y falla de forma independiente: si se cae ms-customer, las cuentas se
  siguen viendo.
- ❌ No hay transacciones entre servicios: el onboarding necesita compensación (ADR-11).
- ❌ Más infraestructura que operar: tres bases de datos.

## ADR-3: JWT RS256 validado con JWKS

**Contexto.** Tres servicios deben validar la identidad del cliente en cada petición sin llamar a ms-auth.

**Decisión.**
- ms-auth firma un JWT de 15 minutos con **RS256** y publica su clave pública en `/.well-known/jwks.json`.
- Los demás servicios validan la firma, la expiración y el emisor (`iss = nexo-auth`), con el JWKS en caché y
  timeouts cortos.

**Alternativas.**
- *HS256 con un secreto compartido:* más simple, pero cualquier servicio que valide podría también **emitir**
  tokens. Además, rotar el secreto obliga a coordinar a todos.
- *Tokens opacos con introspección:* revocación inmediata, pero cada petición hace una llamada a ms-auth. Suma
  latencia y convierte a ms-auth en un punto único de falla.

**Consecuencias.**
- ✅ La validación es local: si ms-auth cae, se sigue validando con las claves en caché.
- ✅ Solo ms-auth tiene la clave privada.
- ❌ Un access token no se puede revocar antes de expirar; se mitiga con su vida de 15 minutos (ADR-4).

## ADR-4: Refresh token opaco, rotativo y con detección de reutilización

**Contexto.** El access token es corto. La app necesita renovarlo sin pedir la contraseña, y un refresh token
robado no debe valer indefinidamente.

**Decisión.**
- El refresh token son 256 bits aleatorios, con vida de 7 días. En la base de datos solo se guarda su SHA-256.
- **Cada uso lo rota.** Si llega uno ya rotado, se asume robo y se revocan **todas** las sesiones del usuario.

**Alternativas.**
- *Refresh token JWT de larga vida:* no requiere base de datos, pero no se puede revocar.
- *Sin rotación:* más simple, pero un robo pasa desapercibido.

**Consecuencias.**
- ✅ Logout real.
- ✅ El robo de un refresh token se detecta.
- ✅ Una filtración de la base de datos no expone tokens utilizables.
- ❌ Dos renovaciones simultáneas desde la misma app pueden invalidar la sesión. La app las encola: un solo refresh
  a la vez.

## ADR-5: Login cifrado con JWE

**Contexto.** TLS protege el tránsito, pero suele terminar en un balanceador o un proxy intermedio. Las credenciales
quedan en claro dentro de esa infraestructura y pueden terminar en sus logs.

**Decisión.**
- La app cifra el cuerpo del login con la clave pública `enc` del JWKS (RSA-OAEP-256 + A256GCM) y lo envía como
  `application/jose`.
- ms-auth solo acepta ese algoritmo y la clave vigente (`kid`). También acepta JSON, para pruebas.

**Alternativas.**
- *Solo TLS:* es lo habitual y suficiente para muchos casos.
- *Certificate pinning solamente:* protege contra ataques MITM, pero no dentro de la infraestructura propia.

**Consecuencias.**
- ✅ Las credenciales viajan cifradas de punta a punta.
- ❌ Más complejidad en la app.
- ❌ Hay que gestionar la rotación de la clave de cifrado (la app reintenta si cambió el `kid`).

## ADR-6: Transferencias dentro de ms-accounts, en una sola transacción

**Contexto.** El alcance es transferir **entre cuentas propias**. Ambas cuentas están en la misma base de datos.

**Decisión.** `POST /transfers/own` hace todo en **una transacción local**:
1. Bloquea ambas cuentas.
2. Valida que sean del cliente, que estén activas, la moneda y el saldo.
3. Debita y acredita.
4. Registra la transferencia y sus dos movimientos.

**Alternativas.**
- *Servicio de transferencias separado con saga:* necesario para transferencias interbancarias o entre servicios.
  Aquí agregaba consistencia eventual y compensaciones sin necesidad.
- *Contabilidad por eventos (event sourcing):* auditoría perfecta, pero desproporcionado para el alcance.

**Consecuencias.**
- ✅ Consistencia fuerte: o se hace todo, o no se hace nada.
- ✅ El saldo siempre cuadra con los movimientos.
- ❌ Si en el futuro hay transferencias a terceros, se necesitará un servicio de pagos con saga. Ver
  `risks-and-scaling.md`.

## ADR-7: Idempotencia con `Idempotency-Key` y bloqueo pesimista ordenado

**Contexto.** En redes móviles una respuesta puede perderse y la app no sabe si la transferencia se hizo. Un usuario
también puede tocar dos veces. Mover dinero dos veces es inaceptable.

**Decisión.**
- **Idempotencia.** El header `Idempotency-Key` es obligatorio y su unicidad se garantiza por cliente con un
  `UNIQUE`. Se guarda una huella SHA-256 del contenido:
  - Misma clave y mismo contenido: devuelve la transferencia original (200, `Idempotent-Replayed: true`).
  - Misma clave con otro contenido: 409.
- **Concurrencia.** `SELECT … FOR UPDATE` sobre ambas cuentas, **siempre en orden de id**, para que dos
  transferencias cruzadas no provoquen un deadlock. La idempotencia se vuelve a verificar **después** de tomar el
  bloqueo.

**Alternativas.**
- *Bloqueo optimista (versión):* evita esperas, pero con contención genera reintentos y errores que la app tendría
  que manejar.
- *Cola por cuenta:* serializa sin locks, pero exige un broker y vuelve asíncrona una operación que el usuario
  espera ver confirmada.
- *Idempotencia en el gateway o en caché (Redis):* no es transaccional con el débito. Puede quedar registrada la
  clave sin la transferencia, o al revés.

**Consecuencias.**
- ✅ Un doble toque o un reintento nunca duplican dinero. Lo cubre un test con 10 envíos simultáneos con la misma
  clave.
- ✅ No hay deadlocks en transferencias cruzadas. Lo cubre un test con 40 transferencias concurrentes.
- ❌ Una cuenta con muchísimas operaciones simultáneas se serializa (*hot account*). Ver
  `risks-and-scaling.md`.

## ADR-8: Paginación de movimientos por cursor

**Contexto.** La app muestra los movimientos con scroll infinito, y pueden entrar movimientos nuevos mientras el
usuario hace scroll.

**Decisión.** Se pagina por `(booked_at, id)` con un cursor opaco (Base64), usando el índice
`(account_id, booked_at DESC, id DESC)`. Se pide un elemento de más para saber si hay página siguiente, sin `COUNT`.

**Alternativas.**
- *Offset (`page=N`):* simple, pero un movimiento nuevo desplaza las páginas: hay duplicados o faltantes. Además,
  es más lento con offsets grandes.

**Consecuencias.**
- ✅ Sin duplicados ni saltos.
- ✅ Rendimiento constante en cualquier página.
- ❌ No se puede saltar a la página 7 ni mostrar el total (no se necesita en móvil).

## ADR-9: Experiencia dinámica (SDUI) guardada en base de datos

**Contexto.** El negocio quiere cambiar el home por segmento y lanzar campañas **sin publicar una versión nueva** de
la app.

**Decisión.**
- **Datos.** `experience_components` guarda cada bloque de una pantalla: tipo, posición, `props` en JSONB y filtros
  opcionales (segmento, cliente, ventana de fechas, franja horaria, si es promoción).
- **Composición.** El `ExperienceComposer` filtra según el cliente y sus preferencias, ordena y personaliza los
  textos. La respuesta lleva ETag.
- **App.** Dibuja los tipos que conoce e **ignora los desconocidos**.

**Alternativas.**
- *Firebase Remote Config:* bueno para flags, pero no conoce el segmento del banco ni los datos del cliente.
  Además, sería otra fuente de verdad.
- *Actualizaciones de código over-the-air (Shorebird):* cambian comportamiento, no contenido por cliente, y tienen
  restricciones en las tiendas.
- *Home fijo en la app:* cada cambio requiere publicar una versión nueva.

**Consecuencias.**
- ✅ Activar una fila cambia el home al instante (`scripts/experience.sh`).
- ✅ Se pueden publicar tipos nuevos sin romper versiones viejas de la app.
- ❌ Hoy se configura con SQL: en producción hace falta un backoffice con validación y auditoría.
- ❌ Un componente mal configurado llega a todos. La app lo mitiga con una caché y un layout de respaldo.

## ADR-10: Nginx como gateway

**Contexto.** La app necesita una sola URL. Las rutas internas no deben exponerse. Además, se quería un lugar
común para el rate limit, los timeouts y el correlation-id.

**Decisión.** Nginx enruta por prefijo:
- Bloquea `/internal` y `/actuator` (404).
- Aplica rate limit: 20 logins por minuto por IP y 30 peticiones por segundo en el resto.
- Genera o respeta `X-Correlation-Id`.
- Corta a los 10 s.
- **Nunca reintenta** (un POST de dinero no es seguro de repetir).
- Si un servicio no responde, devuelve un 503 en formato `ProblemDetail`.
- Hace de proxy hacia la API externa de tipo de cambio sin reenviar el token del cliente.

**Alternativas.**
- *Spring Cloud Gateway:* filtros en Java y descubrimiento de servicios, pero es otro servicio JVM que mantener.
- *Kong o un API Gateway gestionado (AWS API Gateway, Apigee):* es lo indicado en producción (planes, cuotas,
  analítica), pero excesivo para el entorno local.

**Consecuencias.**
- ✅ Liviano, declarativo y fácil de auditar.
- ❌ La lógica avanzada (rate limit por usuario, autenticación en el borde) requiere módulos o un gateway gestionado.

## ADR-11: Onboarding síncrono con compensación

**Contexto.** Registrarse crea datos en tres servicios: credenciales en ms-auth, cliente en ms-customer y cuenta en
ms-accounts. No hay transacción distribuida.

**Decisión.**
- ms-auth provisiona **primero** el cliente y la cuenta, y **solo si ambos responden** crea las credenciales.
- Las provisiones son idempotentes por `customerId`.
- Si la apertura de la cuenta falla, se **descarta el cliente recién creado** (`DELETE /internal/customers/{id}`).
- Cada intento usa un `customerId` nuevo, así que reintentar es seguro.

**Alternativas.**
- *Saga asíncrona con outbox y broker:* más robusta ante caídas largas, pero el usuario no recibe una respuesta
  inmediata ("tu cuenta está lista").
- *Crear las credenciales primero:* si falla después, queda un usuario que puede iniciar sesión pero no tiene
  cuenta.

**Consecuencias.**
- ✅ El usuario sabe al instante si se registró.
- ✅ Nunca quedan credenciales sin cliente.
- ❌ Si falla también la compensación, queda un cliente huérfano (sin usuario, no puede iniciar sesión). Hay que
  limpiarlo con un job.
- ❌ El registro depende de que los tres servicios estén arriba.

## ADR-12: Cifrado en reposo en la aplicación (AES-256-GCM)

**Contexto.** La cédula y el teléfono son datos personales sensibles. Una copia de la base de datos o un acceso
indebido no debería exponerlos.

**Decisión.**
- Se cifran en la aplicación con AES-256-GCM, mediante un `AttributeConverter` de JPA. Formato: `v1:<base64>`, con
  un IV aleatorio por valor.
- La clave sale de `CUSTOMER_DATA_KEY`. Sin ella, el servicio no arranca.
- Una migración Java (`V101`) cifró los datos existentes.

**Alternativas.**
- *Cifrado del disco o de la base de datos (TDE, RDS con KMS):* transparente, pero no protege ante un acceso con
  credenciales de la base de datos ni ante un dump.
- *`pgcrypto` en PostgreSQL:* la clave tendría que viajar en las consultas y podría quedar en los logs de la base
  de datos.

**Consecuencias.**
- ✅ En la base de datos solo hay texto cifrado y autenticado: una alteración se detecta.
- ✅ El prefijo de versión permite rotar la clave.
- ❌ No se puede buscar por cédula; haría falta un *blind index* (HMAC).
- ❌ La gestión de la clave pasa a ser crítica: perderla es perder los datos.

## ADR-13: Notificaciones: ms-customer es el dueño y el aviso es asíncrono tras el commit

**Contexto.** Al completarse una transferencia hay que avisar al cliente. El evento ocurre en ms-accounts, pero el
idioma, la preferencia "recibir notificaciones" y los dispositivos están en ms-customer.

**Decisión.**
- ms-accounts llama a `POST /internal/notifications` de ms-customer **después del commit y en segundo plano**
  (hilos virtuales, timeouts cortos y propagación del correlation-id).
- ms-customer arma el texto en el idioma del cliente, respeta su preferencia y envía a cada dispositivo por FCM.
  Sin credenciales de FCM, escribe la notificación en el log.
- Los tokens que FCM reporta como inválidos se borran.

**Alternativas.**
- *Enviar el push desde ms-accounts:* duplica las preferencias y los dispositivos en dos servicios.
- *Outbox y broker (Kafka, RabbitMQ):* entrega garantizada y desacople total. Es la evolución natural, pero agrega
  infraestructura.

**Consecuencias.**
- ✅ La transferencia nunca espera ni falla por una notificación.
- ✅ Un rollback no genera un aviso falso.
- ❌ Es de mejor esfuerzo: si ms-customer está caído en ese momento, el aviso se pierde. La app muestra el
  comprobante igual.

## ADR-14: Toxiproxy para demostrar la resiliencia

**Contexto.** La prueba pide explicar **y demostrar** el comportamiento con conectividad limitada y caídas parciales.

**Decisión.** Toxiproxy se ubica entre el gateway y cada servicio. Los scripts de `chaos/` agregan latencia, cortan
un servicio o restauran todo usando su API.

**Alternativas.**
- *Detener contenedores:* solo simula caídas, no latencia.
- *Chaos Mesh o Gremlin:* pensados para Kubernetes o producción; excesivos para una demo local.

**Consecuencias.**
- ✅ Demo repetible y en vivo: latencia, timeout del gateway, caída parcial y recuperación.
- ❌ Un salto de red adicional en el entorno local. En producción no se despliega.

## ADR-15: Contrato de API: montos como texto y errores `ProblemDetail`

**Contexto.** La app y el backend deben entenderse sin ambigüedad sobre el dinero y los errores.

**Decisión.**
- **Montos:** texto con dos decimales (`"125.50"`). En el backend son `BigDecimal` y `NUMERIC(19,2)`.
- **Fechas:** ISO-8601 en UTC.
- **Errores:** `ProblemDetail` (RFC 7807) con un `code` estable y el `correlationId`.

**Alternativas.**
- *Montos como número JSON:* se convierten a `double` en el cliente y hay errores de redondeo.
- *Montos en centavos (entero):* evita el redondeo, pero es propenso a confusiones de escala en la interfaz.
- *Errores libres:* la app tendría que interpretar textos.

**Consecuencias.**
- ✅ Sin errores de redondeo.
- ✅ La app traduce cada `code` a un mensaje en su idioma.
- ✅ Soporte puede rastrear cualquier error por su `correlationId`.
- ❌ La app debe convertir el texto a su tipo de dinero.
