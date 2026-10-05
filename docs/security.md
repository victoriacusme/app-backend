# Cómo protejo el backend

Un banco maneja dinero y datos personales, así que la seguridad está pensada en capas: si una falla, la siguiente
sigue protegiendo. Aquí explico cada capa en lenguaje simple y, al final, lo que haría distinto en producción.

## Resumen

| Qué protejo | Cómo |
|---|---|
| Contraseñas | Nunca se guardan; solo una huella irreversible (Argon2id) |
| Intentos de adivinar contraseñas | Bloqueo al quinto intento fallido, y máximo 20 intentos de login por minuto desde una misma IP |
| La sesión | Pase de 15 minutos firmado (JWT) y pase de renovación que cambia en cada uso |
| El login en tránsito | Usuario y contraseña viajan cifrados, además de HTTPS (JWE) |
| Que nadie vea datos ajenos | El backend sabe quién eres por tu sesión, nunca por lo que escribas en la petición |
| Las conversaciones entre servicios | Rutas internas con clave propia y bloqueadas desde afuera |
| Datos personales guardados | Cédula y teléfono cifrados en la base de datos |
| Datos que se muestran | Números de cuenta, cédula y teléfono enmascarados (`****4521`) |
| Los registros del sistema (logs) | Sin contraseñas, tokens ni datos personales |

## Contraseñas e intentos de adivinarlas

- **No guardo contraseñas.** Guardo una huella hecha con Argon2id, un algoritmo diseñado para ser lento de
  adivinar. Ni yo, ni quien acceda a la base de datos, puede recuperar la contraseña original.
- **Al quinto intento fallido, la cuenta se bloquea**, incluso si después se escribe la contraseña correcta.
- **El gateway limita los intentos por IP:** como máximo 20 logins por minuto.
- **No revelo si un usuario existe.** "Usuario inexistente" y "contraseña incorrecta" dan la misma respuesta y
  tardan lo mismo, para que nadie pueda averiguar qué usuarios hay.

## La sesión

- **Pase de acceso (JWT) de 15 minutos**, firmado por `ms-auth` con una clave privada. Los demás servicios lo
  verifican con la clave pública, que guardan en memoria, sin consultar a `ms-auth`.
- **Pase de renovación de 7 días**, para no pedir la contraseña cada 15 minutos.
  - **Cambia cada vez que se usa.**
  - Si alguien usa uno que ya fue cambiado (señal de robo), **cierro todas las sesiones** de ese usuario.
  - En la base de datos guardo solo una huella del pase, no el pase.

## El login viaja cifrado

HTTPS protege la conexión, pero normalmente termina en un equipo intermedio de la infraestructura. Por eso la app,
además, **cifra usuario y contraseña con una clave pública** que publica el backend. Solo `ms-auth` puede
descifrarlos, y nadie en el camino los ve legibles.

## Nadie ve datos de otro cliente

- **El backend sabe quién eres por tu pase de acceso, nunca por un parámetro de la petición.** No hay forma de
  pedir "las cuentas del cliente X".
- **Si pides una cuenta o transferencia ajena, respondo "no existe"**, igual que si de verdad no existiera. Así no
  confirmo que existe.
- **Si intentas transferir desde o hacia una cuenta ajena**, respondo que no te pertenece.

## Conversaciones entre servicios

En el registro y en las notificaciones, los servicios se hablan entre sí por rutas internas (`/internal/...`). Esas
rutas:
- **Exigen una clave interna** que solo conocen los servicios.
- **No aceptan el pase de un cliente**: aunque alguien tenga una sesión válida, no puede llamarlas.
- **Están bloqueadas en el gateway**: desde afuera responden "no existe".

### En producción: certificados entre servicios (mTLS)

Una clave compartida alcanza para el entorno local, pero en producción la reemplazaría por **certificados para cada
servicio** (TLS mutuo). Así:
- Cada servicio demuestra quién es con su propio certificado, que además se renueva solo.
- Puedo autorizar por servicio: por ejemplo, "solo `ms-auth` puede abrir cuentas".
- Si se filtra un secreto, no compromete a todos.

Lo más práctico en Kubernetes es una *service mesh* (Istio o Linkerd), que lo hace sin cambiar el código.

## Datos personales guardados (cédula y teléfono)

- **Los cifro antes de guardarlos** (AES-256-GCM). En la base de datos se ve algo como `v1:TrqNxIOz…`. La
  aplicación los descifra al leerlos y la API los muestra enmascarados.
- **Cada valor se cifra distinto**, aunque dos clientes tengan el mismo teléfono.
- **Si alguien altera un dato cifrado, se detecta**: no se descifra basura.
- **La clave vive fuera del código** (`CUSTOMER_DATA_KEY`). **Sin ella, el servicio no arranca**: prefiero que no
  funcione a que guarde datos en claro.
- **Puedo cambiar la clave en el futuro:** el prefijo `v1:` indica con qué versión se cifró, así que una versión
  `v2:` puede convivir mientras se migran los datos.
- **Lo que no puedo hacer:** buscar clientes por cédula. Si hiciera falta, agregaría una huella de la cédula hecha
  con otra clave (*blind index*).
- **El email no lo cifro**, porque es el canal de contacto y su exposición es menos grave. Se puede cifrar con el
  mismo mecanismo si hiciera falta.

## Los registros del sistema (logs)

- **Nunca registro** contraseñas, tokens, cédulas, teléfonos, emails ni números de cuenta completos. Hay pruebas
  automáticas que lo verifican.
- **Para investigar un problema uso un identificador de seguimiento** (`X-Correlation-Id`). Viaja por todos los
  servicios y aparece en cada registro y en cada mensaje de error. Así puedo seguir el caso de un cliente sin pedirle
  datos personales.

## Claves y secretos

| Secreto | Para qué | En desarrollo |
|---|---|---|
| `JWT_SIGNING_KEY` / `JWT_ENCRYPTION_KEY` | Firmar sesiones y descifrar el login | Se generan al arrancar (se pierden al reiniciar) |
| `INTERNAL_API_KEY` | Conversaciones entre servicios | `dev-internal-key-change-me` |
| `CUSTOMER_DATA_KEY` | Cifrar cédula y teléfono | Una clave fija de desarrollo en el compose |
| `DB_USER` / `DB_PASSWORD` | Acceso a las bases de datos | `nexo` / `nexo` |

**En producción**, todos estos secretos vivirían en una bóveda de claves (AWS Secrets Manager, Vault o similar),
nunca en el repositorio.

## Lo que haría distinto en producción

| Hoy (entorno local) | En producción |
|---|---|
| Claves de desarrollo en el compose | Bóveda de claves por entorno |
| Clave compartida entre servicios | Certificados por servicio (mTLS) |
| HTTP sin cifrar en local | HTTPS en la entrada, y la app verifica el certificado del servidor (*certificate pinning*) |
| Límite de intentos por IP (varios usuarios detrás de una misma red lo comparten) | Límites por usuario o por dispositivo en un gateway gestionado |
| Un pase de acceso no se puede anular antes de sus 15 minutos | Lista de pases anulados, si el negocio lo exige |
