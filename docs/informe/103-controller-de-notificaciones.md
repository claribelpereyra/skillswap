# Tarea 103 — Controller de notificaciones

Actualización: los módulos ahora son `domain`, `application`, `infrastructure`, `controllers` y `app`. Notificaciones usa `IdentityService`, `ControllerResult` y el contrato `NotificationController` con `Page<NotificationView>` (tareas 102 y 106). La migración de notificaciones se renumeró a V9. Las referencias siguientes a los contratos anteriores describen el estado inicial; consultar esos documentos para la integración actual.

`NotificacionController` permite listar con paginación y marcar una notificación como leída. Recibe `BandejaNotificacionEntrada` y una función que entrega el userId activo. No acepta elegir otro destinatario desde la pantalla y consulta la sesión en cada operación. El caso de uso y el repositorio comprueban la pertenencia al marcar lectura.

El resultado usa `Resultado<T>` y `ErrorController`, preparados en la tarea 102. Los errores de datos, sesión ausente, notificación no encontrada y fallos internos conservan el mismo formato. La conexión del resto de los controllers y la clasificación de sus conflictos de negocio siguen pendientes de sus implementaciones y de los errores base de Clari.

La función `Supplier<String>` es un punto de integración con el adaptador de identidad mock de Andy, no una implementación de autenticación. Se conecta por referencia a su método de usuario activo cuando esté disponible. Los identificadores siguen representados como String hasta integrar `UserId` común; la paginación local se llama `PaginaNotificaciones` para no ocupar la clase `Pagina` de Clari. El caso de uso usa `java.time.Clock` inyectado; no se implementa el puerto de reloj de Tomi.

## Estado de las tareas implementadas

| Tarea | Alcance realizado | Integración que falta |
| --- | --- | --- |
| 93 | Evento base, publicador Observer, alta/baja y aislamiento de fallos de suscriptores | Publicar después del commit desde los casos de uso reales |
| 94 | Contrato inmutable con los seis tipos y suscriptor que delega procesamiento | Los emisores de contratación/pago deben formar referencias, detalles y destinatarios correctos |
| 95 | Consulta y cambio de preferencia por usuario; interna por defecto | Conectar la edición de preferencia a la UI/identidad |
| 96 | Fábrica que selecciona entre tres canales inyectados | Registrar sus adaptadores al componer Main |
| 97 | Bandeja paginada y marca de lectura limitada al dueño e idempotente | Conectar pantallas |
| 99 | Migración V2, entidades, mapper y repositorios JPA con pruebas H2 | Verificar MySQL real y coordinar número de migración antes del merge |
| 100 | Email, push e interna mock; procesamiento y registro del resultado de envío | Probar eventos emitidos por operaciones reales |
| 102 | Formato común de resultados y traducción de errores conocidos | Aplicarlo a controllers del equipo y ampliar errores de negocio |
| 103 | Controller de bandeja, pruebas de delegación, sesión y entradas | Adaptador de identidad y Mock UI Kit |

## Límites de entrega

El procesamiento conserva un registro por evento/destinatario y no lo reenvía al recibir el mismo evento. Se registran estados PENDIENTE, ENVIADA o FALLIDA. El envío simulado y la actualización de su estado no son atómicos: una caída puede dejar PENDIENTE. No hay reintentos automáticos ni garantía de entrega exactamente una vez. Una falla de notificaciones después del commit no debe reportarse como rollback del pago o contratación ya confirmados.

Los repositorios de notificaciones y preferencias usan transacciones locales por operación, separadas del negocio confirmado. No sustituyen la unidad de trabajo que implementa Clari para reserva, pago y cancelación.

Los mocks conservan los avisos enviados en memoria para inspeccionarlos en demos/pruebas. La bandeja persistente conserva los avisos de todos los canales. La prueba de integración de infraestructura verifica un evento con email y push, el canal interno por defecto y la deduplicación. No representa todavía el recorrido completo de la tarea 107.

Las pruebas aplican Flyway y luego Hibernate con `validate` sobre H2 en modo MySQL, incluyendo una carrera de inserción duplicada. El motor Docker no estaba disponible durante esta revisión: no se afirma que las pruebas hayan corrido contra MySQL.
