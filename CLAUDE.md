# SkillSwap — contexto para Claude Code

TP obligatorio de Procesos de Desarrollo de Software (UADE), equipo de 5. Marketplace de mentorías, servicios y desafíos donde se paga con créditos ficticios (SkillCoins). El foco de evaluación es el **backend**: dominio, casos de uso, controllers, arquitectura, persistencia y pruebas. No se evalúa diseño visual.

El enunciado completo está en `docs/enunciado/TP_PDS_SkillSwap.pdf`. Ante cualquier duda de alcance o reglas, leelo antes de asumir.

## Stack y restricciones

- Java 17, Maven multi-módulo.
- Hibernate/JPA + MySQL 8. **Prohibido Spring Boot** (y cualquier framework de inyección o web).
- Migraciones con Flyway. **Nunca** `hibernate.hbm2ddl.auto=create` o `update`: el esquema lo maneja Flyway y Hibernate solo valida.
- Sin HTTP, sin API REST: los controllers son clases Java normales que llama la UI dentro de la misma app.
- La UI la provee la cátedra (Mock UI Kit, Swing o JavaFX, con un `MockSkillSwapController`). No se rediseñan pantallas: se implementan los controllers reales y se conectan por interfaces.
- Tests: JUnit 5, Mockito, AssertJ, H2 para persistencia, JaCoCo para cobertura.

## Comandos

```bash
docker compose up -d                       # MySQL local (localhost:3306, db/user/pass: skillswap)
mvn verify                                 # compila, tests y reporte de cobertura
mvn -pl skillswap-dominio test             # tests de un solo módulo
```

La app se corre con `com.uade.skillswap.app.Main` (módulo `skillswap-app`); al arrancar aplica las migraciones. Conexión por variables `SKILLSWAP_DB_URL`, `SKILLSWAP_DB_USER`, `SKILLSWAP_DB_PASSWORD`.

## Arquitectura (Clean Architecture, obligatoria)

Cada capa es un módulo Maven. Las dependencias solo apuntan hacia adentro y están forzadas por los POMs:

| Módulo | Paquete base | Contiene | Depende de |
| --- | --- | --- | --- |
| `skillswap-dominio` | `com.uade.skillswap.dominio` | Entidades, value objects, reglas, políticas, estados, eventos | nada |
| `skillswap-aplicacion` | `com.uade.skillswap.aplicacion` | Casos de uso, puertos de entrada (`puertos.entrada`) y salida (`puertos.salida`), límites transaccionales | dominio |
| `skillswap-infraestructura` | `com.uade.skillswap.infraestructura` | Entidades JPA, repositorios, mappers, Flyway, adaptadores mock (identidad, email, push, pasarela de SkillCoins) | aplicación |
| `skillswap-controllers` | `com.uade.skillswap.controllers` | Controllers delgados que validan entrada y delegan en puertos de entrada | aplicación |
| `skillswap-app` | `com.uade.skillswap.app` | Mock UI Kit + `Main` (composition root: único lugar donde se instancian implementaciones concretas) | controllers, infraestructura |

Reglas que no se negocian:

- **El dominio no tiene anotaciones JPA.** Las entidades JPA viven en infraestructura (por ejemplo `PublicacionJpaEntity`) y se mapean a las de dominio. Cada entidad JPA nueva se registra en `skillswap-infraestructura/src/main/resources/META-INF/persistence.xml`.
- Los repositorios son interfaces (puertos de salida) en aplicación; infraestructura los implementa.
- Los controllers no tienen SQL, ni reglas de negocio, ni conocen repositorios o entidades JPA. Usan command objects para no tener listas largas de parámetros y manejan los errores de aplicación de forma uniforme.
- La UI conoce interfaces de controllers, nunca repositorios ni entidades JPA.
- **No agregar dependencias entre módulos** ni librerías nuevas sin consultarme primero.

## Dominio: reglas clave

**Perfil**: se registra referenciando el `userId` del servicio de identidad externo (mock). Tiene nombre visible, biografía, zona horaria, habilidades, intereses y disponibilidad. El perfil público muestra reputación, publicaciones activas e historial resumido. Un usuario puede ser cliente, proveedor o ambos.

**Publicación** (oferta de mentoría/servicio de un profesional, o desafío de una organización): título, descripción, categoría, modalidad (remota/presencial), duración estimada, cupo, precio base en SkillCoins, etiquetas.

- Estados: `BORRADOR` (editable, no visible) → `PUBLICADA` (visible, recibe solicitudes) ⇄ `PAUSADA` (solo la ve el autor, no recibe solicitudes) → `CERRADA` (no se reabre). `CANCELADA` requiere motivo.
- Publicar exige título, categoría, precio no negativo y al menos una franja de disponibilidad.
- Las transiciones inválidas las rechaza el dominio.

**Contratación**: el cliente solicita indicando mensaje, franja elegida y objetivo. El proveedor acepta o rechaza; aceptar crea una reserva y bloquea el cupo. El cliente paga con SkillCoins. Una reserva se reprograma antes de empezar solo si ambas partes aceptan. Al completar, cada parte puede calificar una vez.

| Estado | Acciones válidas |
| --- | --- |
| `PENDIENTE` | aceptar o rechazar |
| `ACEPTADA` | pagar, cancelar, proponer reprogramación |
| `CONFIRMADA` | iniciar, reprogramar o cancelar según política |
| `EN_CURSO` | completar |
| `COMPLETADA` | calificar, consultar comprobante |
| `RECHAZADA` / `CANCELADA` | solo consultar historial |

Reglas de negocio mínimas:

- El autor no puede solicitar su propia publicación; una publicación no publicada no admite solicitudes.
- No se reserva una franja ocupada ni se supera el cupo. La aceptación bloquea el cupo de forma transaccional, con **optimistic locking** (`@Version`) para evitar sobreventa.
- Débito y crédito de SkillCoins en una **única transacción**. Sin saldos negativos ni doble cobro.
- Cancelación devuelve 100 %, 50 % o 0 % según anticipación configurable.
- Promociones acumulables pueden bajar el precio, pero el total nunca es negativo.
- Solo los participantes de una contratación completada califican, una vez cada uno, y nadie se califica a sí mismo. La reputación se recalcula desde calificaciones verificadas; no se edita a mano.
- Toda transición de estado registra fecha, actor y motivo en un **historial inmutable**.
- La baja de perfil es lógica: no se borran operaciones históricas.
- Los controllers que modifican datos validan permisos con el `userId` del servicio de identidad mock.
- Eventos que notifican (email, push o interna, según preferencia del usuario): solicitud recibida, aceptación, pago confirmado, reprogramación, cancelación, sesión completada.

Búsqueda: por texto, categoría, modalidad, rango de precio, habilidad y reputación mínima; orden por relevancia, reputación, precio o fecha; **todos los listados paginados**. Al menos dos estrategias de recomendación intercambiables (afinidad por habilidades y popularidad reciente).

Wallet: el usuario consulta saldo y movimientos de SkillCoins. Notificaciones: bandeja del usuario y marcar como leída.

## Controllers mínimos (nombres orientativos del enunciado)

| Controller y operación | Propósito |
| --- | --- |
| `PerfilController.crearPerfil(...)` / `verPerfil(id)` | Crear perfil profesional; ver perfil y reputación |
| `PublicacionController.crearBorrador(...)` / `cambiarEstado(...)` | Crear publicación en borrador; ejecutar una transición |
| `BusquedaController.buscar(filtros)` | Buscar, filtrar, ordenar y paginar |
| `RecomendacionController.recomendar(usuario)` | Recomendaciones para el usuario |
| `ContratacionController.solicitar / aceptar / pagar / cancelar / completar` | Ciclo de la contratación (aceptar reserva cupo, cancelar aplica política de reintegro) |
| `CalificacionController.calificar(...)` | Registrar calificación verificada |
| `NotificacionController.listar(usuario)` | Listar notificaciones |

La Mock UI además necesita: wallet (saldo y movimientos) y marcar notificación como leída. Login simulado: la UI pasa el `userId` activo en cada operación.

Recorrido que se demuestra en vivo: publicar → solicitar → aceptar → pagar → completar → calificar.

**Fuera de alcance**: autenticación real (registro, contraseñas, OAuth/JWT), dinero real, chat/videollamadas, despliegue productivo, diseño visual.

## Patrones

Mínimo cuatro, cada uno resolviendo una variación concreta y con tests que lo respalden. Candidatos naturales: State (estados de publicación/contratación), Strategy (recomendación, política de cancelación, precio), Observer (eventos y notificaciones), Adapter (identidad, email, push, pasarela), Decorator (promociones acumulables), Simple Factory (canal de notificación), Facade (perfil resumido o checkout), Composite (categorías y habilidades). Singleton solo con justificación explícita. No agregar patrones por cantidad.

## Persistencia

- Migraciones en `skillswap-infraestructura/src/main/resources/db/migration`, formato `V<n>__descripcion.sql`. **Nunca editar una migración ya mergeada**: siempre una nueva.
- Se persisten: perfiles, habilidades, publicaciones, franjas, solicitudes, contrataciones, movimientos de créditos, calificaciones, notificaciones e historial de estados.
- Límites transaccionales explícitos en aceptación, reserva, pago y cancelación.
- Modelar bien cardinalidades, claves, unicidad e índices.
- Evitar N+1 en las consultas principales (fetch joins, entity graphs o proyecciones) y dejar comentado por qué.
- Datos de demo reproducibles en su propia migración (por ejemplo `V100__datos_demo.sql`).

## Tests

- Unitarios de dominio y de cada estrategia/política.
- Casos de uso con puertos simulados (Mockito).
- Persistencia contra H2 con datos controlados.
- Integración controllers → casos de uso → persistencia.
- Casos negativos obligatorios: transición inválida, saldo insuficiente, cupo agotado, doble pago, calificación no autorizada.
- Cobertura objetivo: 80 % en dominio y aplicación.
- Nombres de test descriptivos en español (por ejemplo `noPermiteRestarMasQueElSaldo`).

Casos de aceptación del enunciado, que tienen que poder demostrarse:

1. Publicar una mentoría válida y encontrarla con búsqueda filtrada.
2. Rechazar la publicación de una oferta incompleta.
3. Solicitar, aceptar y pagar con saldo suficiente.
4. Dos aceptaciones simultáneas no consumen el último cupo.
5. Pago con saldo insuficiente rechazado sin modificar saldos ni estado.
6. Cancelar y calcular reintegro con dos políticas distintas.
7. Cambiar la estrategia de recomendación sin tocar el caso de uso consumidor.
8. Dos tipos de notificación ante un mismo evento, con adaptadores mock.
9. Promociones acumulables sin precio negativo.
10. Completar, calificar una sola vez y recalcular reputación.

## Hitos

1. **Diseño**: alcance, historias, modelo de dominio, diagrama de clases (StarUML), decisiones de arquitectura y patrones candidatos.
2. **Núcleo**: dominio y casos de uso principales con puertos y tests unitarios.
3. **Infraestructura**: JPA/MySQL, repositorios y adaptadores mock; flujo transaccional real.
4. **Producto**: UI conectada a controllers, documentación, tests y versión final.

Cada entrega incluye código, UML en StarUML (`docs/uml/`) e informe PDF de decisiones (`docs/informe/`).

**Se entrega todo junto el martes 17 de noviembre de 2026.** Los hitos son etapas internas con fecha objetivo: Hito 1 dom 25/10, Hito 2 dom 1/11, Hito 3 dom 8/11, Hito 4 vie 13/11; del 14 al 16/11 informe, UML final y ensayo de defensa.

## Flujo de Git

- `develop` es la rama default y de integración. `main` solo recibe un PR desde `develop` al cerrar cada hito (merge commit, nunca squash), y después se taggea `hito-N` sobre `main`.
- Las ramas de trabajo salen de `develop` y vuelven por PR con squash: `feature/`, `fix/`, `test/`, `refactor/`, `docs/`.
- Commits: `tipo(capa): descripción en imperativo`, en inglés (tipos `feat`, `fix`, `test`, `refactor`, `docs`, `chore`).
- PRs: **título en inglés** con el formato de los commits (con squash queda como commit en `develop`) y **descripción en español** siguiendo `.github/pull_request_template.md`.
- Detalle completo en `CONTRIBUTING.md`.

**Reglas para vos (Claude Code):**

- **No hagas commit ni push.** Preparás los cambios, me mostrás `git status` y `git diff --stat`, y yo commiteo.
- Antes de empezar una tarea, chequeá en qué rama estoy. Si estoy en `develop` o `main`, proponé crear una rama de trabajo con el prefijo que corresponda.
- No edites `docs/uml/*.mdj` (StarUML no se mergea bien; lo maneja una sola persona).
- Antes de dar algo por terminado, corré `mvn verify` y decime si pasó.
- Si un cambio implica una decisión de diseño (nuevo patrón, nueva entidad, cambio de capa), explicame la alternativa elegida y por qué en dos o tres líneas: me sirve para el informe y la defensa.

## Equipo

Cada uno es dueño de un área de punta a punta (dominio, caso de uso, persistencia y controller). El reparto completo y las tareas están en el plan y la planilla de tareas del equipo (Google Drive).

| Persona | Área |
| --- | --- |
| Clari (yo) | Contratación: ciclo completo, reserva y cupo con optimistic locking, historial de estados, núcleo común, unidad de trabajo y configuración de JPA |
| Luqui | Publicaciones, franjas, catálogo de categorías y búsqueda. Dueño del `.mdj` de StarUML |
| Tomi | Wallet, pago, políticas de cancelación, promociones y recomendación |
| Andy | Perfiles, calificaciones, reputación, identidad mock y permisos |
| Ian | Notificaciones, manejo de errores en controllers, `Main` e integración del Mock UI Kit |

Revisión cruzada fija: Clari revisa a Luqui, Luqui a Andy, Andy a Ian, Ian a Tomi y Tomi a Clari.

## Pendientes

- [x] Alinear `CONTRIBUTING.md` y `.github/workflows/ci.yml` con el flujo `develop`/`main` (PR #1).
- [x] Rulesets en GitHub sobre `main` y `develop` con el check `build` requerido.
- [ ] Integrar el Mock UI Kit cuando lo entregue la cátedra (ver si es Swing o JavaFX; JavaFX requiere dependencias de OpenJFX).
- [ ] Hito 1: modelo de dominio y diagrama de clases.
