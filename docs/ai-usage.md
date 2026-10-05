# Uso de IA en el desarrollo

Este backend se desarrolló con **Claude Code** (asistente de programación de Anthropic, modelo Claude Opus) como
herramienta principal. Este documento explica para qué se usó, qué decidió la persona y qué la IA, cómo se verificó
lo que produjo y qué errores cometió.

## Cómo se trabajó

El trabajo siguió el plan por fases de la prueba. En cada fase:

1. **La persona fijó el alcance y las prioridades:** qué fase seguía, qué entraba y qué se postergaba (por ejemplo,
   dejar la Fase 8 de Grafana como documento en vez de implementarla).
2. **La IA propuso un diseño** con sus alternativas antes de escribir código: por qué paginar por cursor, por qué el
   bloqueo pesimista ordenado, quién debía ser el dueño de las notificaciones. Las decisiones quedaron en
   [`decisions.md`](decisions.md).
3. **La IA implementó** el código, los tests y la configuración, siguiendo el estilo existente del repositorio.
4. **Se verificó contra el sistema real**, no solo con tests: el stack completo levantado con Docker, con pruebas de
   punta a punta por el gateway con `curl` y escenarios de caos.
5. **La persona revisó**, hizo los commits y los PR, y probó desde la app.

## Para qué se usó

| Área | Uso |
|---|---|
| Diseño | Opciones con trade-offs para cada decisión de arquitectura; la persona eligió |
| Código | Dominio, casos de uso, adaptadores, migraciones Flyway, semillas de datos y configuración de seguridad |
| Tests | Unitarios con Mockito (por pedido explícito, todos los tests con colaboradores usan Mockito), de controladores y de integración con Testcontainers, incluidos los de concurrencia |
| Infraestructura | `docker-compose.yml`, gateway Nginx, Toxiproxy, scripts de caos y de demo, y el CI |
| Diagnóstico | Errores de entorno: red que bloquea Docker Hub, contexto de Docker Desktop, cambio de rama con archivos sin commitear, y lectura de logs para ubicar fallas |
| Documentación | README y documentos de `docs/` |
| Explicaciones | Conceptos que la persona pidió entender: imagen vs. contenedor, para qué sirve el gateway, qué es CI, qué es el servicio externo |

El **front** (Flutter) se desarrolló en otra sesión de IA. Los dos lados se coordinaron a través del contrato de API.
Por ejemplo, el front reportó un error real del backend (la meta de ahorro de otro cliente) y se corrigió con un test
que lo reproduce.

## Cómo se verificó lo que produjo la IA

No se aceptó código solo porque compilaba. Cada cambio se validó en tres niveles:

1. **Tests automáticos:** unos 170 tests en los tres servicios. Los de integración usan PostgreSQL real
   (Testcontainers), no una base de datos en memoria, para probar el SQL, los bloqueos y las migraciones tal como
   corren en producción.
2. **Pruebas de punta a punta** sobre el stack levantado, a través del gateway: login, cuentas, transferencias,
   idempotencia con envíos simultáneos, registro con un servicio caído, cifrado verificado leyendo la base de datos
   y logs revisados en busca de datos personales.
3. **Revisión de afirmaciones:** antes de documentar algo, se comprobó en el código o en el sistema. Por ejemplo, se
   verificaron los nombres reales de las métricas antes de escribir `monitoring.md`, y el código de la app antes de
   describir su comportamiento.

## Errores que cometió la IA y cómo se detectaron

La IA se equivoca, y estos casos muestran por qué la verificación es indispensable:

| Error | Cómo se detectó | Impacto evitado |
|---|---|---|
| Timeouts de Hikari escritos como `3s` (la propiedad espera milisegundos) | Test de integración con Testcontainers | ms-accounts no habría arrancado |
| Columna `SMALLINT` mapeada como `Integer` en JPA | Test de integración | ms-customer no habría arrancado |
| Semilla con número `V100`: las migraciones nuevas (`V2`) quedaban bloqueadas en bases ya existentes | Despliegue real sobre la base de datos de desarrollo | Cada fase nueva habría tumbado el servicio |
| Gateway que no leía los cambios de configuración (montaje de un archivo suelto en Docker) | Prueba de punta a punta (404 en `/transfers`) | Rutas nuevas que "no existían" |
| Componente SDUI configurado por segmento con la cuenta de un cliente concreto | Lo reportó el front | Un cliente nuevo veía una cuenta ajena |
| Onboarding sin compensación: quedaban clientes huérfanos | Prueba real con ms-accounts detenido | Datos inconsistentes entre servicios |
| `toString()` de records que imprimía contraseñas y datos personales | Auditoría de logs pedida en la Fase 7 | Fuga de datos personales en los logs |
| Afirmación de que un token alterado era aceptado (falso positivo del propio script de prueba) | Repetir la prueba alterando bytes reales | Un falso hallazgo de seguridad |
| Variables de shell que funcionan en bash pero no en zsh | Error 127 al ejecutar | Diagnóstico equivocado de Docker |

## Qué no se delegó

- **Las decisiones de alcance y prioridad:** qué fases hacer, qué dejar fuera y cuándo integrar.
- **La aceptación final:** commits, PR y pruebas desde la app las hizo la persona.
- **Las acciones irreversibles:** la IA no hizo commits, push ni borrado de datos sin indicación. Antes de cambiar de
  rama con archivos pendientes, respaldó los cambios y verificó que no se perdiera nada.

## Lecciones

- **La IA acelera mucho la parte mecánica** (adaptadores, mapeos, tests, configuración), pero el valor está en
  verificar contra el sistema real. Varios errores solo aparecieron con una base de datos real o con el stack
  levantado.
- **Pedir alternativas antes del código** produjo mejores decisiones que pedir directamente la implementación.
- **Los tests de integración con PostgreSQL real** fueron los que más errores atraparon. Una base de datos en
  memoria no los habría detectado.
