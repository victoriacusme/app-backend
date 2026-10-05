# Decisiones que tomé y por qué

Aquí explico las decisiones técnicas más importantes del backend. Para cada una cuento **qué decidí**, **por qué**,
**qué otras opciones consideré** y **qué gané y qué pagué** con esa elección. Ninguna decisión es gratis: todas
tienen un costo, y prefiero dejarlo escrito.

| # | Decisión |
|---|---|
| 1 | [Separar cada servicio en capas (arquitectura hexagonal)](#1-separar-cada-servicio-en-capas-arquitectura-hexagonal) |
| 2 | [Tres servicios, cada uno con su propia base de datos](#2-tres-servicios-cada-uno-con-su-propia-base-de-datos) |
| 3 | [Sesiones con tokens firmados (JWT RS256)](#3-sesiones-con-tokens-firmados-jwt-rs256) |
| 4 | [Un token de renovación que cambia en cada uso](#4-un-token-de-renovación-que-cambia-en-cada-uso) |
| 5 | [El login viaja cifrado (JWE)](#5-el-login-viaja-cifrado-jwe) |
| 6 | [Las transferencias se hacen en un solo paso](#6-las-transferencias-se-hacen-en-un-solo-paso) |
| 7 | [Una transferencia nunca se ejecuta dos veces](#7-una-transferencia-nunca-se-ejecuta-dos-veces) |
| 8 | [Los movimientos se cargan por "marcador" y no por número de página](#8-los-movimientos-se-cargan-por-marcador-y-no-por-número-de-página) |
| 9 | [El home se arma desde la base de datos (SDUI)](#9-el-home-se-arma-desde-la-base-de-datos-sdui) |
| 10 | [Una sola puerta de entrada: Nginx](#10-una-sola-puerta-de-entrada-nginx) |
| 11 | [El registro deshace lo que alcanzó a crear si algo falla](#11-el-registro-deshace-lo-que-alcanzó-a-crear-si-algo-falla) |
| 12 | [Cifro la cédula y el teléfono antes de guardarlos](#12-cifro-la-cédula-y-el-teléfono-antes-de-guardarlos) |
| 13 | [Las notificaciones salen después de confirmar la transferencia](#13-las-notificaciones-salen-después-de-confirmar-la-transferencia) |
| 14 | [Un simulador de fallas para demostrar la resiliencia](#14-un-simulador-de-fallas-para-demostrar-la-resiliencia) |
| 15 | [Montos como texto y errores con un código claro](#15-montos-como-texto-y-errores-con-un-código-claro) |

---

## 1. Separar cada servicio en capas (arquitectura hexagonal)

**Qué decidí.** Cada servicio tiene tres partes:
- **Dominio:** las reglas del banco ("no se puede gastar más del saldo", "al quinto intento fallido se bloquea").
- **Aplicación:** los casos de uso ("transferir", "iniciar sesión").
- **Infraestructura:** lo técnico (base de datos, HTTP, notificaciones).

Las reglas no dependen de ninguna tecnología.

**Por qué.** Las reglas de un banco son lo más importante y lo que más hay que probar. Así las pruebo en
milisegundos, sin levantar una base de datos.

**Otras opciones.** Las capas clásicas (controlador → servicio → repositorio) son más simples, pero las reglas
terminan mezcladas con el código de la base de datos.

**Gano:** pruebas rápidas y la posibilidad de cambiar una pieza técnica sin tocar las reglas. Por ejemplo, cambiar
las notificaciones simuladas por Firebase no tocó el dominio.
**Pago:** más archivos y algo de código repetido entre servicios.

## 2. Tres servicios, cada uno con su propia base de datos

**Qué decidí.**
- `ms-auth`: usuarios y sesiones.
- `ms-customer`: perfil, preferencias, home y notificaciones.
- `ms-accounts`: cuentas, movimientos y transferencias.

Ninguno lee la base de datos de otro. Se relacionan solo por el identificador del cliente, que viaja en el token.

**Por qué.** La prueba pide microservicios, y estos tres temas son naturalmente independientes.

**Otras opciones.**
- *Una sola aplicación bien organizada (monolito modular):* para este tamaño habría sido más simple de operar, pero
  no cumplía el requisito.
- *Una base de datos compartida:* facilita las consultas, pero ata los servicios entre sí.

**Gano:** si se cae el servicio de perfiles, las cuentas y las transferencias siguen funcionando.
**Pago:** no puedo hacer una operación que abarque varios servicios a la vez (lo resuelvo en la decisión 11), y hay
tres bases de datos que mantener.

## 3. Sesiones con tokens firmados (JWT RS256)

**Qué decidí.** Al iniciar sesión, `ms-auth` entrega un token de 15 minutos firmado con su clave privada. Los otros
servicios lo verifican con la clave pública, que guardan en memoria, **sin preguntarle a `ms-auth`**.

**Por qué.** Si cada petición tuviera que consultar a `ms-auth`, todo dependería de él y sería más lento.

**Otras opciones.**
- *Una clave secreta compartida (HS256):* más simple, pero cualquier servicio podría **fabricar** tokens.
- *Tokens sin información que se consultan cada vez:* permiten cerrar sesiones al instante, pero suman una llamada
  en cada petición.

**Gano:** si `ms-auth` se cae, quien ya inició sesión sigue usando la app.
**Pago:** no puedo invalidar un token antes de que venza. Por eso dura solo 15 minutos.

## 4. Un token de renovación que cambia en cada uso

**Qué decidí.** Para no pedir la contraseña cada 15 minutos, la app tiene un token de renovación de 7 días.
- **Cada vez que se usa, se cambia por uno nuevo.**
- Si alguien usa uno viejo, asumo que fue robado y **cierro todas las sesiones** del usuario.
- En la base de datos solo guardo una huella del token, nunca el token.

**Por qué.** Un token de larga duración robado es peligroso. Así, el robo se detecta.

**Otras opciones.** Un token de renovación que no cambia es más simple, pero un robo pasaría inadvertido.

**Gano:** cierre de sesión real y detección de robos.
**Pago:** si la app pidiera dos renovaciones a la vez, se cerraría la sesión. Por eso la app hace una sola a la vez.

## 5. El login viaja cifrado (JWE)

**Qué decidí.** Además de HTTPS, la app cifra usuario y contraseña con una clave pública del backend. Solo
`ms-auth` puede leerlos.

**Por qué.** HTTPS suele terminar en un equipo intermedio de la infraestructura (un balanceador). A partir de ahí,
las credenciales viajarían legibles y podrían quedar en sus registros.

**Otras opciones.** Solo HTTPS. Es lo habitual y suficiente en muchos casos, pero en un banco prefiero una capa más.

**Gano:** la contraseña no es legible en ningún punto intermedio.
**Pago:** más complejidad en la app, y la clave hay que renovarla de forma coordinada.

## 6. Las transferencias se hacen en un solo paso

**Qué decidí.** Como solo hay transferencias **entre cuentas propias**, y ambas cuentas están en la misma base de
datos, todo ocurre en un único paso indivisible: reservar las dos cuentas, validar, debitar, acreditar y registrar
los movimientos. O se hace todo, o no se hace nada.

**Otras opciones.** Un servicio de pagos aparte, con varios pasos coordinados (una "saga"). Es necesario para
transferir a otros bancos, pero aquí agregaba complejidad sin beneficio.

**Gano:** el saldo siempre cuadra con los movimientos, sin estados intermedios.
**Pago:** para transferencias a terceros habría que construir ese servicio de pagos.

## 7. Una transferencia nunca se ejecuta dos veces

**El problema.** El cliente toca "Transferir" y la señal se cae: la app no sabe si se hizo. O el cliente toca dos
veces seguidas.

**Qué decidí.**
- **Un "número de recibo" por intento.** La app genera uno único (`Idempotency-Key`) y el backend lo guarda junto
  con una huella del contenido:
  - Mismo recibo y mismos datos: devuelvo la transferencia original y **no muevo dinero otra vez**.
  - Mismo recibo con otros datos: rechazo con un conflicto (409).
- **La app nunca reintenta sola una operación de dinero.** Si no recibió respuesta, le muestra al cliente "No
  pudimos confirmar el resultado" y un botón **"Verificar estado"**, que reenvía el mismo recibo.
- **Turnos para las cuentas.** Mientras una transferencia trabaja con dos cuentas, las reservo siempre en el mismo
  orden. Así, dos transferencias cruzadas (A→B y B→A) no se bloquean mutuamente.

**Otras opciones.**
- *Detectar conflictos y reintentar (bloqueo optimista):* con mucha actividad genera errores que la app tendría
  que manejar.
- *Guardar los recibos fuera de la base (por ejemplo, en Redis):* podría quedar guardado el recibo sin la
  transferencia, o al revés.

**Gano:** lo probé con 10 envíos simultáneos del mismo recibo (se creó una sola transferencia) y con 40
transferencias cruzadas al mismo tiempo (sin bloqueos).
**Pago:** si una sola cuenta recibiera miles de operaciones por segundo, esperarían en fila. En banca personas no
pasa.

## 8. Los movimientos se cargan por "marcador" y no por número de página

**El problema.** La app carga los movimientos a medida que el usuario hace scroll. Si entra un movimiento nuevo
mientras tanto, la paginación por número ("dame la página 2") repite o se salta filas.

**Qué decidí.** Cada página devuelve un marcador ("continúa desde aquí") basado en la fecha del último movimiento.

**Otras opciones.** Paginación por número de página: más simple, pero con duplicados y cada vez más lenta.

**Gano:** sin duplicados y la misma velocidad en cualquier página.
**Pago:** no se puede saltar a la página 7 ni mostrar el total (en el celular no hace falta).

## 9. El home se arma desde la base de datos (SDUI)

**El problema.** El negocio quiere mostrar cosas distintas a cada tipo de cliente y lanzar campañas **sin publicar
una versión nueva de la app**.

**Qué decidí.** El home es una lista de bloques guardados en la base de datos. Cada bloque tiene un tipo (saludo,
cuentas, banner, tipo de cambio…), un orden y para quién aplica: un segmento, un cliente, unas fechas, un horario.
El backend arma la lista para cada cliente, y la app dibuja los bloques que conoce e **ignora los que no**.

**Otras opciones.**
- *Firebase Remote Config:* bueno para encender o apagar funciones, pero no conoce los datos del banco.
- *Home fijo en la app:* cada cambio obliga a publicar en las tiendas.

**Gano:** activar una campaña es cambiar una fila; se ve al instante (`scripts/experience.sh`).
**Pago:** hoy se configura con SQL; en producción haría falta una pantalla de administración con revisión. Un
bloque mal cargado llega a todos (la app lo mitiga con un home de respaldo).

## 10. Una sola puerta de entrada: Nginx

**Qué decidí.** La app habla con una sola dirección (el gateway), que:
- Reparte cada petición al servicio correcto.
- Bloquea las rutas internas.
- Limita los intentos de login por IP.
- Corta las peticiones que tardan más de 10 segundos.
- Responde un error claro si un servicio no está.
- Nunca reintenta por su cuenta (un pago no se puede repetir a ciegas).

**Otras opciones.**
- *Spring Cloud Gateway:* otro servicio Java más que mantener.
- *Un gateway gestionado (Kong, AWS API Gateway):* lo usaría en producción, pero es demasiado para el entorno
  local.

**Gano:** liviano, simple y fácil de revisar.
**Pago:** funciones avanzadas, como límites por usuario, requerirían un gateway más completo.

## 11. El registro deshace lo que alcanzó a crear si algo falla

**El problema.** Registrarse crea cosas en tres servicios: el usuario, el cliente y su cuenta. No hay forma de
hacerlo todo "de una vez" entre servicios distintos.

**Qué decidí.**
- Crear **primero** el cliente y la cuenta, y **al final** el usuario.
- Si la cuenta no se puede abrir, **borro el cliente** que alcancé a crear y respondo "intenta de nuevo".
- Cada intento usa un identificador nuevo, así que reintentar es seguro.

**Otras opciones.** Un registro en segundo plano con mensajes entre servicios: más robusto ante caídas largas, pero
el usuario no sabría al instante si su cuenta quedó lista.

**Gano:** nunca queda un usuario que pueda iniciar sesión sin cuenta. Lo probé apagando `ms-accounts` en medio de
un registro.
**Pago:** el registro necesita los tres servicios funcionando.

## 12. Cifro la cédula y el teléfono antes de guardarlos

**Qué decidí.** La cédula y el teléfono se guardan cifrados (AES-256-GCM). En la base de datos se ve algo como
`v1:TrqNxIOz…`. La clave vive fuera del código y, sin ella, el servicio no arranca.

**Por qué.** Si alguien obtiene una copia de la base de datos, no debería poder leer datos personales.

**Otras opciones.** Cifrar solo el disco. Protege si roban el disco, pero no si alguien entra a la base de datos
con un usuario válido.

**Gano:** una copia de la base de datos no expone esos datos, y si alguien los altera, se detecta.
**Pago:** no puedo buscar clientes por cédula, y perder la clave sería perder esos datos.

## 13. Las notificaciones salen después de confirmar la transferencia

**Qué decidí.** Cuando una transferencia se confirma, `ms-accounts` avisa a `ms-customer` **en segundo plano**.
`ms-customer` es quien conoce el idioma, la preferencia de notificaciones y los teléfonos del cliente; arma el
mensaje y lo envía por Firebase.

**Por qué.** La transferencia no debe esperar ni fallar por culpa de un aviso. Y un aviso nunca debe salir si la
transferencia no se completó.

**Otras opciones.** Una cola de mensajes (Kafka o RabbitMQ) garantiza la entrega, pero agrega infraestructura. Es el
siguiente paso natural.

**Gano:** transferencias rápidas y sin avisos falsos.
**Pago:** si `ms-customer` está caído justo en ese momento, ese aviso se pierde. La transferencia sí queda hecha.

## 14. Un simulador de fallas para demostrar la resiliencia

**Qué decidí.** Puse Toxiproxy entre el gateway y los servicios. Con un comando le agrego lentitud a un servicio o
lo "desconecto", y muestro en vivo cómo reacciona la app.

**Otras opciones.** Apagar contenedores: solo simula caídas, no lentitud.

**Gano:** una demo repetible de lo que pide la prueba: conectividad limitada y caídas parciales.
**Pago:** un paso extra de red en el entorno local. En producción no se instala.

## 15. Montos como texto y errores con un código claro

**Qué decidí.**
- **Montos:** viajan como texto (`"125.50"`).
- **Errores:** siempre tienen la misma forma, con un código fijo (`insufficient-funds`, `account-not-owned`…) y un
  identificador para rastrearlos.

**Por qué.**
- Los números con decimales pierden precisión en el celular: 0,1 + 0,2 no da exactamente 0,3.
- Con un código fijo, la app muestra el mensaje correcto en el idioma del cliente.

**Otras opciones.** Montos en centavos (enteros): evitan el problema, pero es fácil confundirse de escala en la
interfaz.

**Gano:** cero errores de redondeo y errores fáciles de explicar y de rastrear.
**Pago:** la app tiene que convertir el texto a su tipo de dinero.
