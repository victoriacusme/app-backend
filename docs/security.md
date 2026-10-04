# Seguridad del backend de Nexo

Este documento resume los controles de seguridad del backend (ms-auth, ms-customer, ms-accounts y el gateway), cómo
se configuran y qué falta para producción.

## Resumen de controles

| Área | Control | Dónde |
|---|---|---|
| Contraseñas | Argon2id (m=16 MiB, t=2, p=1) | ms-auth `Argon2PasswordHasher` |
| Fuerza bruta | Bloqueo tras 5 intentos fallidos (por usuario) y rate limit de 20 logins/min (por IP) | ms-auth y gateway |
| Sesión | JWT RS256 de 15 min y refresh token opaco de 7 días, rotativo y con detección de reutilización | ms-auth |
| Login cifrado | JWE (RSA-OAEP-256 + A256GCM) con la clave `enc` publicada en el JWKS | ms-auth `JweLoginDecrypter` |
| Validación de tokens | Firma (JWKS de ms-auth con caché), expiración y emisor (`iss = nexo-auth`) | `SecurityConfig` de cada micro |
| Autorización | El cliente siempre sale del `sub` del JWT, nunca de un parámetro | `CurrentCustomer` |
| Servicio a servicio | `/internal/**` con API key (`X-Internal-Api-Key`), comparada en tiempo constante; bloqueado en el gateway | `InternalApiKeyFilter` |
| Datos en reposo | Cédula y teléfono cifrados con AES-256-GCM; refresh tokens guardados como hash SHA-256 | ms-customer y ms-auth |
| Datos expuestos | Cuenta (`****4521`), cédula y teléfono enmascarados en las respuestas | DTOs de respuesta |
| Logs | Sin contraseñas, tokens ni datos personales; trazabilidad con `X-Correlation-Id` | Toda la plataforma |
| Superficie expuesta | El gateway solo publica rutas conocidas; `/internal` y `/actuator` responden 404 | `gateway/nginx.conf` |

## Autenticación

- **Contraseñas:** se guardan con Argon2id, con los parámetros recomendados por OWASP y Spring Security 5.8+. El hash
  incluye la sal y los parámetros, así que pueden endurecerse más adelante sin migrar los hashes existentes.
- **Usuario inexistente:** se verifica igual contra un hash ficticio, para que el tiempo de respuesta no revele si el
  usuario existe. La respuesta es siempre la misma: `401 invalid-credentials`.
- **Bloqueo:** al quinto intento fallido la cuenta queda bloqueada (`423 user-locked`), incluso para la contraseña
  correcta. El contador se reinicia con un login exitoso.
- **Access token:** JWT firmado con RS256, de 15 minutos. Su `sub` es el `customerId` que usan ms-customer y
  ms-accounts. Al ser corto, no hace falta una lista de revocación.
- **Refresh token:** son 256 bits aleatorios y en la BD solo se guarda su SHA-256. Cada uso lo rota. Si se presenta
  uno ya rotado (señal de robo), se revocan **todas** las sesiones del usuario.

### Login cifrado (JWE)

La app puede enviar el login como JWE compacto (`Content-Type: application/jose`), cifrado con la clave pública `enc`
del JWKS. Así las credenciales viajan cifradas de punta a punta, aunque TLS termine en un proxy o un balanceador
intermedio, y no quedan en claro en ningún log de esa infraestructura. Solo se acepta `alg=RSA-OAEP-256` con
`enc=A256GCM` y la clave vigente (`kid`).

## Autorización

- El cliente se identifica **solo** por el `sub` del JWT. Ningún endpoint acepta un `customerId` de la petición.
- Al consultar una cuenta, movimiento o transferencia de otro cliente se responde `404`, igual que si no existiera,
  para no revelar recursos ajenos.
- Al transferir desde o hacia una cuenta ajena se responde `403 account-not-owned`.
- Cada micro valida la firma, la expiración y el emisor del token. Las claves del JWKS quedan en caché, así que una
  caída breve de ms-auth no impide seguir validando tokens.

## Comunicación entre servicios

Durante el onboarding, ms-auth llama a `POST/DELETE /internal/customers` y `POST /internal/accounts`. Estas rutas:

- Exigen el header `X-Internal-Api-Key`. Se compara en tiempo constante y, si la clave no está configurada, se
  rechaza todo (falla cerrado).
- No procesan tokens Bearer: un JWT de cliente no sirve para llamarlas.
- Responden `404` desde el gateway, porque solo se alcanzan por la red interna de Docker.

### mTLS en producción

La API key compartida es suficiente para el entorno local, pero en producción conviene reemplazarla (o
complementarla) con **TLS mutuo**:

- **Con service mesh (recomendado en Kubernetes):** Istio o Linkerd emiten un certificado por servicio, lo rotan
  automáticamente y cifran todo el tráfico este-oeste sin cambiar el código. Las `AuthorizationPolicy` permiten
  declarar que, por ejemplo, solo `ms-auth` puede llamar a `/internal/accounts`.
- **Sin mesh:** Spring Boot permite configurar SSL bundles con `server.ssl.client-auth=need` en los micros y un
  keystore con certificado de cliente en el `RestClient` de ms-auth. Los certificados los emite una CA interna
  (por ejemplo, Vault PKI) con vida corta y rotación automática.
- **Identidad:** con mTLS la identidad del llamador sale del certificado (SPIFFE ID o el CN), no de un secreto
  compartido. Así se puede autorizar por servicio y una filtración de la API key deja de ser crítica.

## Datos en reposo

### Cédula y teléfono (ms-customer)

- Se cifran con **AES-256-GCM** mediante un `AttributeConverter` de JPA (`EncryptedStringConverter`). El dominio y los
  casos de uso solo ven el valor en claro; en la BD solo hay `v1:<base64(iv || ciphertext || tag)>`.
- Cada valor usa un IV aleatorio: dos clientes con el mismo teléfono tienen textos cifrados distintos.
- GCM autentica los datos: si alguien altera un valor en la BD, el descifrado falla en lugar de devolver basura.
  El prefijo `v1:` también va autenticado.
- **Clave:** `CUSTOMER_DATA_KEY`, en Base64 de 32 bytes (`openssl rand -base64 32`). Si falta o no es válida, ms-customer
  **no arranca**. El `docker-compose.yml` trae una clave fija solo para desarrollo.
- **Migración:** `V2` amplía las columnas y `V101` (migración Java, porque necesita la clave) cifra los datos que estaban
  en claro. Es idempotente.
- **Rotación:** el prefijo de versión permite introducir `v2:` con una clave nueva. Durante la transición se leen ambas
  versiones y un job re-cifra los datos de `v1:`; después se retira la clave anterior.
- **Búsquedas:** un índice sobre texto cifrado con IV aleatorio no sirve para buscar. Si hiciera falta buscar por
  cédula (por ejemplo, para detectar duplicados en el onboarding), se agregaría un *blind index*: una columna con el
  HMAC-SHA256 de la cédula calculado con una clave distinta.
- **Email:** no se cifra porque es el canal de contacto y su exposición es de menor impacto. Si el análisis de riesgo
  lo pide, se cifra con el mismo mecanismo.

### Otros datos

- Contraseñas: Argon2id (ver arriba).
- Refresh tokens: solo su SHA-256.
- Saldos y movimientos: en claro. Su protección depende del control de acceso a la BD y del cifrado de disco del
  proveedor (por ejemplo, RDS con KMS).

## Datos en tránsito

- **Local:** el gateway escucha en HTTP (`:8080`).
- **Producción:** TLS 1.2+ termina en el balanceador o en el gateway, con HSTS. Los micros solo son accesibles por la
  red interna (con mTLS, ver arriba).
- **App:** *certificate pinning* en el cliente HTTP (flavor de producción) para mitigar ataques MITM con certificados
  emitidos indebidamente.

## Logs y datos personales

- No se registran contraseñas, tokens, cédulas, teléfonos, emails ni números de cuenta completos.
- Los comandos y DTOs con datos sensibles (`LoginCommand`, `RegisterCommand`, `NewCustomer`, `Customer`,
  `LoginRequest`, `RegisterRequest`, `NewCustomerRequest`, `RefreshTokenRequest`) tienen un `toString()` que solo
  muestra identificadores. Los tests `CommandsLoggingTest` y `CustomerTest` lo verifican.
- Las notificaciones registran el id de la transferencia y el monto, nunca los números de cuenta.
- El log de acceso del gateway registra la ruta, el estado, la latencia y el correlation-id; no registra cuerpos ni
  headers de autorización.
- Para dar soporte se usa el `X-Correlation-Id`: viaja por el gateway y los micros, aparece en cada línea de log y en
  el campo `correlationId` de los errores. Un caso se sigue sin pedirle datos personales al cliente.

## Secretos

| Variable | Uso | Valor en desarrollo |
|---|---|---|
| `JWT_SIGNING_KEY` | Clave privada RSA para firmar JWT | Vacía: se genera una efímera en cada arranque |
| `JWT_ENCRYPTION_KEY` | Clave privada RSA para el login JWE | Vacía: se genera una efímera en cada arranque |
| `INTERNAL_API_KEY` | API key de `/internal/**` | `dev-internal-key-change-me` |
| `CUSTOMER_DATA_KEY` | AES-256 de los datos personales | Clave fija del `docker-compose.yml` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de las BD | `nexo` / `nexo` |

En producción todos estos secretos salen de un gestor de secretos (AWS Secrets Manager, Vault o similar), nunca del
repositorio. Las claves JWT efímeras invalidan las sesiones en cada reinicio de ms-auth: para una demo estable conviene
fijarlas en un `.env` (ver `.env.example`).

## Riesgos conocidos y siguientes pasos

| Riesgo | Mitigación actual | Siguiente paso |
|---|---|---|
| Secretos de desarrollo en el compose | Solo para local; documentados | Gestor de secretos por entorno |
| API key compartida entre servicios | Solo red interna, comparación en tiempo constante | mTLS con identidad por servicio |
| Rate limit por IP (clientes detrás de un mismo NAT) | Bloqueo adicional por usuario en ms-auth | Rate limit por dispositivo o usuario en un API gateway gestionado |
| Access token no revocable antes de expirar | Vida de 15 min y revocación de refresh tokens | Lista de revocación por `jti` si el negocio lo exige |
| Sin TLS en local | Entorno de desarrollo | TLS en el balanceador y mTLS interno |
| Sin búsquedas sobre la cédula cifrada | No se necesitan hoy | Blind index con HMAC |
