# Tarea 89 — Bosquejo de casos de uso y puertos

Fuente: consigna SkillSwap, secciones 4 a 10, 12, 13 y 15; historias de la tarea 88.

Este documento propone contratos de aplicación para trasladar a StarUML. Los nombres y operaciones son orientativos: todavía no existen implementaciones de estos casos de uso en el checkout revisado. Describir otras áreas permite diseñar sus límites; no implica implementar tareas de otros integrantes. No se modifica el modelo compartido `.mdj`.

## Capas y dirección de dependencias

| Elemento | Ubicación existente | Responsabilidad |
| --- | --- | --- |
| Interfaces de entrada | `skillswap-aplicacion/.../puertos/entrada` | Contratos que invocan los controllers |
| Casos de uso | `skillswap-aplicacion/.../casosdeuso` | Implementar entradas y coordinar dominio, repositorios y transacciones |
| Interfaces de salida | `skillswap-aplicacion/.../puertos/salida` | Necesidades de persistencia, identidad y servicios externos |
| Reglas y eventos | `skillswap-dominio` | Invariantes, estados, políticas y hechos de negocio |
| Adaptadores de salida | `skillswap-infraestructura` | Implementar salidas con JPA/MySQL y proveedores mock |
| Controllers | `skillswap-controllers` | Validar entradas y permisos; delegar por interfaces de entrada |
| Presentación y ensamblado | `skillswap-app` | UI y composición de implementaciones en Main |

Las interfaces de salida pertenecen a aplicación, aunque las implemente infraestructura. Dominio no importa aplicación, controllers ni JPA. Los contratos no exponen `EntityManager`, entidades JPA ni componentes Swing/JavaFX.

## Convenciones de los contratos propuestos

- Cada operación que modifica información recibe el `actorUserId` obtenido de identidad mock. El controller valida permisos de entrada y el caso de uso verifica la pertenencia y las autorizaciones necesarias sobre los datos cargados; no confía en un identificador arbitrario ingresado en la UI.
- Los comandos agrupan los datos de entrada y las consultas devuelven DTO inmutables. Los identificadores y value objects definitivos se acordarán con los responsables del dominio.
- `Pagina<T>` contiene elementos, número de página, tamaño y total de elementos. Se validan página no negativa y tamaño positivo con un máximo acotado. El orden incluye un desempate estable por identificador.
- Todas las consultas que devuelven listados reciben paginación, incluidos historial, movimientos y recomendaciones. Los detalles individuales devuelven un DTO o un error de recurso inexistente.
- Se distinguen errores de entrada inválida, recurso inexistente, falta de permiso, conflicto de negocio y fallo técnico. El formato común que verá la UI corresponde a la tarea 102; este bosquejo no fija una jerarquía de excepciones Java.
- Las reglas de saldo, cupo, precio y transiciones viven en dominio. Los casos de uso coordinan su ejecución y la persistencia.

## Catálogo de entradas y casos de uso por área

Cada nombre terminado en `Entrada` representa una interfaz. Cada nombre terminado en `CasoDeUso` representa una clase que realiza esa interfaz. Una interfaz puede tener varias operaciones coherentes del mismo propósito; el bosquejo no exige una clase por cada método.

| Área | Puerto de entrada | Operaciones y datos principales | Implementación propuesta | Puertos de salida usados |
| --- | --- | --- | --- | --- |
| Perfiles | `GestionPerfilEntrada` | `crear(actor, datos)`, `editar(actor, datos)`, `darDeBaja(actor)` | `GestionPerfilCasoDeUso` | `RepositorioPerfiles`, `UnidadDeTrabajo`, `Reloj` |
| Perfil público | `ConsultaPerfilEntrada` | `verPerfil(id)`, `listarPublicaciones(id, pagina)`, `listarHistorial(id, pagina)` | `ConsultaPerfilCasoDeUso` | `ConsultaPerfiles` |
| Publicaciones | `GestionPublicacionEntrada` | `crearBorrador(actor, datos)`, `editar(actor, id, datos)`, `cambiarEstado(actor, id, destino, motivo)` | `GestionPublicacionCasoDeUso` | `RepositorioPublicaciones`, `RepositorioPerfiles`, `UnidadDeTrabajo`, `Reloj` |
| Búsqueda | `BusquedaEntrada` | `buscar(filtros, orden, pagina)`; texto, categoría, modalidad, precio, habilidad y reputación mínima | `BusquedaCasoDeUso` | `ConsultaPublicaciones` |
| Recomendación | `RecomendacionEntrada` | `recomendar(actor, pagina)` | `RecomendacionCasoDeUso` | `RepositorioPerfiles`, `ConsultaPublicaciones` |
| Solicitudes | `SolicitudEntrada` | `solicitar(actor, publicacionId, mensaje, franja, objetivo)`, `rechazar(actor, id, motivo)` | `SolicitudCasoDeUso` | `RepositorioPublicaciones`, `RepositorioContrataciones`, `UnidadDeTrabajo`, `PublicadorEventos`, `Reloj` |
| Aceptación y reserva | `AceptacionEntrada` | `aceptar(actor, contratacionId)` | `AceptacionCasoDeUso` | `RepositorioContrataciones`, `RepositorioPublicaciones`, `UnidadDeTrabajo`, `PublicadorEventos`, `Reloj` |
| Pago | `PagoEntrada` | `pagar(actor, contratacionId)` | `PagoCasoDeUso` | `RepositorioContrataciones`, `RepositorioWallets`, `UnidadDeTrabajo`, `PublicadorEventos`, `Reloj` |
| Cancelación | `CancelacionEntrada` | `cancelar(actor, contratacionId, motivo)` | `CancelacionCasoDeUso` | `RepositorioContrataciones`, `RepositorioPublicaciones`, `RepositorioWallets`, `UnidadDeTrabajo`, `PublicadorEventos`, `Reloj` |
| Reprogramación | `ReprogramacionEntrada` | `proponer(actor, id, nuevaFranja)`, `responder(actor, id, acepta)` | `ReprogramacionCasoDeUso` | `RepositorioContrataciones`, `RepositorioPublicaciones`, `UnidadDeTrabajo`, `PublicadorEventos`, `Reloj` |
| Ejecución | `EjecucionContratacionEntrada` | `iniciar(actor, id)`, `completar(actor, id)` | `EjecucionContratacionCasoDeUso` | `RepositorioContrataciones`, `UnidadDeTrabajo`, `PublicadorEventos`, `Reloj` |
| Historial y comprobante | `ConsultaContratacionEntrada` | `ver(actor, id)`, `listarHistorial(actor, id, pagina)`, `verComprobante(actor, id)` | `ConsultaContratacionCasoDeUso` | `ConsultaContrataciones` |
| SkillCoins | `ConsultaWalletEntrada` | `consultarSaldo(actor)`, `listarMovimientos(actor, pagina)` | `ConsultaWalletCasoDeUso` | `ConsultaWallets` |
| Calificaciones | `CalificacionEntrada` | `calificar(actor, contratacionId, puntaje, comentario)` | `CalificacionCasoDeUso` | `RepositorioContrataciones`, `RepositorioCalificaciones`, `RepositorioPerfiles`, `UnidadDeTrabajo`, `Reloj` |
| Preferencias | `PreferenciaNotificacionEntrada` | `consultar(actor)`, `cambiar(actor, canal)` | `PreferenciaNotificacionCasoDeUso` | `RepositorioPreferencias`, `UnidadDeTrabajo` |
| Bandeja | `BandejaNotificacionEntrada` | `listar(actor, pagina)`, `marcarLeida(actor, notificacionId)` | `BandejaNotificacionCasoDeUso` | `RepositorioNotificaciones`, `UnidadDeTrabajo`, `Reloj` |
| Reacción a eventos | `ProcesarEventoNotificableEntrada` | `procesar(evento)`; entrada interna invocada por Observer, sin acción de la UI | `ProcesarEventoNotificableCasoDeUso` | `RepositorioPreferencias`, `RepositorioNotificaciones`, `CanalNotificacion`, `UnidadDeTrabajo`, `Reloj` |

La recomendación recibe por constructor una estrategia del dominio; la cancelación recibe su política; el cálculo de precio usa las políticas/promociones del dominio. Estas interfaces de comportamiento no son adaptadores de persistencia ni deben seleccionar implementaciones concretas dentro del caso de uso. La configuración externa y la selección al componer la aplicación corresponden a las tareas 101 y 104.

## Contratos de salida

Las operaciones siguientes describen capacidades mínimas, no firmas Java definitivas.

| Puerto | Capacidades | Adaptador propuesto |
| --- | --- | --- |
| `RepositorioPerfiles` | Buscar por perfil o userId y guardar; preservar la baja lógica | `RepositorioPerfilesJpa` |
| `RepositorioPublicaciones` | Cargar y guardar publicaciones, disponibilidad y cupos con control de versión | `RepositorioPublicacionesJpa` |
| `RepositorioContrataciones` | Cargar y guardar solicitud/contratación, reserva, propuestas e historial inmutable | `RepositorioContratacionesJpa` |
| `RepositorioWallets` | Cargar wallets y registrar saldos y movimientos dentro de la misma transacción | `RepositorioWalletsJpa` |
| `RepositorioCalificaciones` | Consultar y guardar por contratación y autor, con unicidad | `RepositorioCalificacionesJpa` |
| `RepositorioPreferencias` | Buscar y guardar canal por userId | `RepositorioPreferenciasJpa` |
| `RepositorioNotificaciones` | Guardar aviso, consultar página por destinatario y recuperar para marcar lectura; detectar evento/destinatario duplicado | `RepositorioNotificacionesJpa` |
| `ConsultaPerfiles` | Proyectar perfil público, reputación, publicaciones activas e historial paginado | `ConsultaPerfilesJpa` |
| `ConsultaPublicaciones` | Filtrar, ordenar y paginar; obtener candidatos para recomendación | `ConsultaPublicacionesJpa` |
| `ConsultaContrataciones` | Proyectar detalle, historial paginado y comprobante, limitados al participante | `ConsultaContratacionesJpa` |
| `ConsultaWallets` | Proyectar saldo y movimientos paginados del usuario | `ConsultaWalletsJpa` |
| `UnidadDeTrabajo` | Ejecutar una operación atómica, confirmar o revertir y coordinar acciones posteriores al commit | `UnidadDeTrabajoJpa` |
| `PublicadorEventos` | Publicar hechos del dominio confirmados para suscriptores registrados | `PublicadorEventosEnMemoria` |
| `CanalNotificacion` | Enviar un aviso a su destinatario por un canal reemplazable | `EmailMock`, `PushMock`, `InternaMock` |
| `IdentidadActual` | Obtener el userId activo de la sesión simulada; consumidor: adaptador de entrada/controller | `IdentidadMock` |
| `Reloj` | Proveer el instante actual sin llamadas directas al reloj del sistema en reglas temporales | `RelojSistema` |

Las consultas proyectadas evitan cargar grafos JPA completos. Separarlas de los repositorios de escritura es una propuesta de interfaces para permitir consultas eficientes, no exige bases separadas ni infraestructura CQRS adicional. La estrategia de recomendación y su consulta deberán acordar cómo obtener candidatos suficientes antes de ordenar y paginar; no se debe recomendar únicamente sobre una página arbitraria de resultados de búsqueda.

No se necesita una pasarela externa para transferir SkillCoins: débito y crédito se coordinan sobre persistencia local en una única transacción. Una futura pasarela no reemplaza esa garantía.

## Flujos y límites transaccionales

### Aceptar y reservar

1. El controller obtiene el actor y llama a `AceptacionEntrada`.
2. El caso de uso abre una unidad de trabajo y carga contratación y publicación.
3. Verifica que el actor sea el proveedor y delega en dominio la transición, la disponibilidad de franja y el consumo de cupo.
4. Guarda contratación, reserva, cupo e historial de estado como una operación atómica. La versión o restricción de concurrencia evita consumir dos veces el último cupo; un conflicto revierte toda la operación.
5. Después del commit publica el evento de aceptación. Si la operación falla, no publica éxito.

### Pagar

1. Carga contratación y wallets dentro de una unidad de trabajo.
2. Verifica al cliente y aplica las reglas de estado, saldo suficiente y pago único.
3. Guarda débito, crédito, movimientos, nuevo estado e historial en la misma transacción. El control concurrente debe impedir doble pago y pérdida de actualizaciones de saldos.
4. Publica el pago confirmado después del commit; los suscriptores notifican a las partes.

### Cancelar y reprogramar

- Cancelar coordina estado, historial, liberación de reserva/cupo cuando corresponda y reintegro según anticipación y política. Se confirma todo junto o se revierte todo.
- Proponer una nueva franja no equivale a confirmarla. Cuando se reúnen las aceptaciones requeridas, se verifica disponibilidad y se intercambia la reserva de forma atómica, conservando la anterior si falla.
- Ambas operaciones usan `Reloj` y dominio para validar tiempos; la UI no calcula políticas ni decide transiciones.

### Calificar

Comprueba que la contratación esté completada, que el actor participe y califique a la otra parte una sola vez. Guarda calificación y reputación recalculada en la misma unidad de trabajo. La unicidad debe sostenerse también ante solicitudes concurrentes.

### Notificar y consultar bandeja

1. Un suscriptor recibe uno de los seis eventos de la tarea 88 y llama a `ProcesarEventoNotificableEntrada`.
2. El caso de uso determina destinatarios a partir del hecho confirmado, consulta preferencias y registra el aviso por evento/destinatario.
3. Una fábrica selecciona el `CanalNotificacion` correspondiente; el caso de uso consume la abstracción. Email, push e interna son adaptadores mock.
4. La bandeja consulta por actor y pagina; marcar lectura verifica pertenencia y es idempotente.

El registro persistente y el envío por un adaptador externo no forman automáticamente una única transacción. La implementación deberá definir cómo registrar fallos y reintentos sin anunciar como fallida una contratación que ya se confirmó. Un publicador en memoria puede perder eventos si el proceso termina entre commit y publicación; este bosquejo no garantiza entrega exactamente una vez ni incorpora una outbox. La deduplicación persistente requiere una restricción única, no solo una consulta previa.

## Guía para trasladar a StarUML

Crear paquetes `dominio`, `aplicacion.entrada`, `aplicacion.casosdeuso`, `aplicacion.salida`, `controllers`, `infraestructura` y `app`, correspondientes a los módulos reales.

| Origen | Relación UML | Destino | Multiplicidad o criterio |
| --- | --- | --- | --- |
| Controller | Dependencia de uso | Puerto de entrada | Referencia inyectada por cada función utilizada |
| Caso de uso | Realización | Puerto de entrada | Implementa las operaciones del contrato |
| Caso de uso | Asociación navegable | Puerto de salida | Una referencia por colaborador requerido; sin composición del adaptador |
| Adaptador | Realización | Puerto de salida | Sustituible por otro adaptador del mismo contrato |
| Caso de uso | Dependencia de uso | Entidad, value object o política del dominio | No conoce su representación JPA |
| Publicador Observer | Asociación navegable | Interfaz de suscriptor | Un publicador tiene `0..*` suscriptores |
| Suscriptor de notificaciones | Dependencia de uso | `ProcesarEventoNotificableEntrada` | Delega procesamiento sin reglas de contratación |
| Fábrica de canales | Dependencia de creación/selección | Implementaciones de `CanalNotificacion` | Selecciona una implementación para el canal solicitado |
| Main | Dependencia de creación | Controllers, casos de uso y adaptadores concretos | Ensambla el grafo e inyecta interfaces |

Las multiplicidades de entidades (perfil, publicación, contratación, etc.) deben venir del modelo de dominio de sus responsables; estas asociaciones entre servicios no las sustituyen. Usar notas o estereotipos para identificar Observer, Adapter, Simple Factory y las políticas Strategy. No dibujar dependencias de aplicación hacia adaptadores concretos.

## Validación prevista y dependencias

- Unitarias de casos de uso con puertos simulados: éxito, permisos, reglas rechazadas, rollback y ausencia de eventos de éxito en operaciones fallidas.
- Persistencia: último cupo concurrente, doble pago, atomicidad de saldos, unicidad de calificaciones y deduplicación de avisos.
- Controllers: entrada inválida, errores uniformes y paginación sin acceso a repositorios.
- Integración de notificaciones: mismo evento con cliente email y proveedor push, como exige el caso 8.
- Recorrido final: publicar, solicitar, aceptar, pagar, completar y calificar, conservando persistencia e historial.

La tarea 89 queda resuelta como bosquejo documental. La implementación de cada área depende de sus responsables; la validación de contratos con la UI depende del Mock UI Kit (tarea 92). El archivo StarUML compartido debe editarse coordinadamente según CONTRIBUTING.md. No se agregan interfaces Java vacías ni dependencias Maven para presentar como implementado este diseño.
