# Tarea 104 — Composición de la aplicación

`Main` abre `CompositionRoot` con la conexión de entorno, un `Clock` UTC y las variables externas. El root crea una sola factory JPA y arma repositorios, adaptadores, casos de uso y controllers. `ApplicationRuntime` es dueño de la factory y se cierra al terminar; la arquitectura existente admite una sola ejecución por proceso (`EntityManagerContext` compartido).

La UI recibe `ApplicationControllers`, un conjunto de interfaces de controllers. No recibe repositories, `EntityManager`, canales ni agregados de dominio. `SessionController` permite elegir un `UserId` para el login simulado; no verifica contraseñas ni representa autenticación real.

## Eventos y transacciones

`BookingServiceImpl` conserva los eventos de la instancia modificada, guarda la contratación y registra la publicación en `UnitOfWork.afterCommit`. Incluye solicitud, aceptación, pago, cancelación y finalización; una reprogramación aceptada produce un evento sin cambiar de estado.

`JpaUnitOfWork` une operaciones anidadas a la raíz. Solo la raíz confirma y ejecuta callbacks, después de cerrar su EntityManager. Un rollback elimina los callbacks. Registrar callbacks sin una unidad activa produce un error. Un fallo de entrega se registra en el log y no convierte una operación confirmada en un error ni impide los callbacks restantes.

`BookingNotificationObserver` transforma los eventos en `EventoNotificable`, mantiene un UUID estable para reentregas de la misma transición y avisa a ambas partes. No envía avisos por inicio, rechazo ni rechazo de propuesta de reprogramación. Se conserva la deduplicación por evento/destinatario en persistencia.

La estrategia de entrega es síncrona después del commit y sin reintento automático. Se registran fallos; no existe un outbox durable y un cierre del proceso entre commit y callback puede perder un aviso. Los mocks no envían correo ni push reales.

## Evidencia

- `FullJourneyIntegrationTest`: usa el mismo root que Main, controllers reales, JPA, H2 y Flyway.
- `JpaUnitOfWorkTest`: commit raíz, rollback, callbacks anidados, aislamiento de fallos y rechazo de callbacks huérfanos.
- `BookingObserverTest`: aislamiento y registro de fallos, sin reproducir eventos al cargar una contratación.
- `BookingNotificationObserverTest`: los seis eventos, destinatarios, IDs estables y transiciones que no notifican.
