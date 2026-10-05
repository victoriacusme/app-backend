# Cómo usé la IA en este proyecto

Desarrollé el backend con **Claude Code** (el asistente de programación de Anthropic, modelo Claude Opus) como
herramienta principal. Aquí cuento cómo trabajé con la IA, qué decidí yo y qué hizo ella, cómo comprobé lo que
produjo y en qué se equivocó.

## Cómo trabajé

Seguí el plan por fases de la prueba. En cada fase:

1. **Yo definía qué tocaba y qué quedaba fuera.** Por ejemplo, decidí dejar la Fase 8 (Grafana) como documento y no
   implementarla todavía.
2. **Le pedía a la IA opciones antes que código.** Para cada decisión importante comparaba alternativas: cómo
   paginar los movimientos, cómo evitar transferencias duplicadas, qué servicio debía enviar las notificaciones.
   Mis elecciones quedaron en [`decisions.md`](decisions.md).
3. **La IA escribía** el código, las pruebas y la configuración, siguiendo el estilo del proyecto.
4. **Comprobaba todo contra el sistema real**, no solo con pruebas: levantaba todo con Docker y probaba los flujos
   de punta a punta.
5. **Yo revisaba, hacía los commits y los PR**, y probaba desde la app.

## Ejemplos de lo que le pedí

| Lo que pedí | Qué hizo la IA |
|---|---|
| *"Todos los test deben ser con Mockito"* | Reescribió las pruebas que usaban dobles hechos a mano y agregó las que faltaban |
| *"Valida que la fase 0 y 1 estén integradas para continuar"* | Corrió todas las pruebas y el flujo completo de login contra el sistema levantado, e informó qué faltaba |
| *"Realiza la fase 5"* | Propuso el diseño del home dinámico, lo implementó con pruebas y lo verificó con los tres usuarios de prueba |
| Le pasé un error que encontró el front: un cliente nuevo veía la meta de ahorro de otra persona | Lo reprodujo con una prueba, lo corrigió y verificó con un registro real |
| *"Explícame qué es CI"*, *"¿para qué sirve el gateway?"* | Me explicó los conceptos en lenguaje simple antes de seguir |

## Para qué la usé

| Área | Uso |
|---|---|
| Diseño | Comparar alternativas con sus pros y contras; la decisión final fue mía |
| Código | Reglas de negocio, casos de uso, base de datos, seguridad y configuración |
| Pruebas | Pruebas unitarias con Mockito y pruebas con una base de datos real (Testcontainers), incluidas las de concurrencia |
| Infraestructura | Docker Compose, gateway, simulador de fallas, scripts y CI |
| Diagnóstico | Problemas del entorno: mi red bloqueaba descargas de Docker Hub, el cambio a Docker Desktop, archivos sin commitear al cambiar de rama, y la lectura de registros para encontrar fallas |
| Documentación | El README y los documentos de esta carpeta |
| Aprender | Me explicó conceptos que no conocía |

El **front** (Flutter) lo desarrollé en otra sesión con un asistente de IA. Los dos lados se coordinaron a través
del contrato de la API.

## Qué información compartí con la IA

- **Solo código, registros y datos de prueba** (los usuarios ficticios ana, carlos y lucía).
- **Ninguna clave real ni dato de clientes reales.** Las claves que aparecen en el proyecto son solo de
  desarrollo.

En un banco real usaría una versión empresarial de la herramienta, con garantías sobre el manejo de los datos.

## Cómo comprobé lo que hizo

No aceptaba código solo porque compilaba. Lo comprobaba en tres niveles:

1. **Pruebas automáticas:** unas 170 en los tres servicios. Las de base de datos usan un PostgreSQL real (no una
   base simulada), para probar los bloqueos y las migraciones tal como funcionan en producción.
2. **Pruebas de punta a punta** con todo levantado: login, transferencias, el mismo recibo enviado varias veces a la
   vez, registro con un servicio apagado, datos cifrados revisados directamente en la base y registros revisados en
   busca de datos personales.
3. **Verificar antes de documentar:** antes de escribir algo, comprobaba que fuera cierto en el código o en el
   sistema.

## En qué se equivocó la IA

La IA se equivoca, y estos casos muestran por qué hay que verificar:

| Error | Cómo lo detecté | Qué habría pasado |
|---|---|---|
| Configuró un tiempo de espera con el formato equivocado (`3s` en lugar de milisegundos) | Una prueba con base de datos real | El servicio de cuentas no habría arrancado |
| Usó un tipo de dato que no coincidía con la base de datos | Una prueba con base de datos real | El servicio de clientes no habría arrancado |
| Numeró los datos de prueba de forma que bloqueaba las actualizaciones futuras de la base | Al desplegar sobre mi base de desarrollo | Cada fase nueva habría roto el servicio |
| El gateway no tomaba los cambios de configuración | Una prueba de punta a punta (las rutas nuevas daban "no existe") | Rutas que parecían no existir |
| Configuró la meta de ahorro de un cliente para todos los clientes jóvenes | Lo detectó el front | Un cliente nuevo veía la cuenta de otra persona |
| El registro dejaba clientes "huérfanos" cuando fallaba a medias | Apagando un servicio en medio de un registro | Datos inconsistentes entre servicios |
| Algunos objetos imprimían contraseñas y datos personales si llegaban a un registro | Una revisión de registros que le pedí en la fase de seguridad | Datos personales expuestos en los registros |
| Reportó una falla de seguridad que no existía (su propia prueba estaba mal) | Repitiendo la prueba de forma correcta | Una falsa alarma de seguridad |

## Lo que no delegué

- **Qué hacer y en qué orden:** qué fases, qué dejar fuera, cuándo integrar.
- **La aprobación final:** los commits, los PR y las pruebas desde la app.
- **Las acciones que no se pueden deshacer:** la IA no hacía commits, push ni borraba datos sin que yo se lo
  pidiera. Antes de cambiar de rama con archivos pendientes, guardaba una copia y verificaba que no se perdiera
  nada.

## Dónde la IA no pudo avanzar sola

- **Comandos con permisos de administrador**, como darme acceso a Docker con `sudo`, y cerrar y volver a abrir mi
  sesión: los hice yo.
- **Ejecutar el CI en GitHub:** la IA lo preparó y validó localmente, pero la primera ejecución real ocurre cuando
  yo subo los cambios.
- **Probar en el celular:** las pruebas en la app las hice yo.

## Lo que aprendí

- **La IA acelera muchísimo la parte repetitiva** (conexiones a la base de datos, pruebas, configuración), pero el
  valor está en **comprobar contra el sistema real**. Varios errores solo aparecieron con una base de datos real o
  con todo levantado.
- **Pedir opciones antes que código** me dio mejores decisiones que pedir directamente la solución.
- **Las pruebas con una base de datos real** fueron las que más errores atraparon.
