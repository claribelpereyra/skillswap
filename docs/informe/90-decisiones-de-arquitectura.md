# Tarea 90 — Decisiones de arquitectura

## Capas y módulos

| Módulo | Responsabilidad | Dependencias internas |
| --- | --- | --- |
| domain | Entidades, reglas, estados, políticas y eventos | Ninguna |
| application | Casos de uso, puertos, DTO y coordinación | domain |
| infrastructure | JPA, Flyway, configuración y adaptadores | application |
| controllers | Validación de identidad/entrada y formato de resultado | application |
| app | Composition root, presentación y pruebas de integración | controllers e infrastructure |

Las dependencias Maven impiden que dominio importe JPA o que un controller conozca una implementación de persistencia. No se usa Spring. La alternativa de mezclar entidades JPA, UI y reglas simplificaría el arranque pero acoplaría todas las pruebas y cambios de tecnología.

## Composición y configuración

Main delega en CompositionRoot: una factory, repositorios y servicios compartidos, Clock inyectable, identidad mock y controllers expuestos por interfaces. ApplicationRuntime administra el cierre. Las variables de entorno se leen en infraestructura y se aplican en el root a RefundPolicy y a la estrategia predeterminada de recomendación; los casos de uso no leen entorno.

La arquitectura existente conserva EntityManagerContext como contexto único por proceso y un EntityManager por hilo/unidad de trabajo. No admite múltiples aplicaciones independientes simultáneas en la misma JVM. Cambiar ese diseño global queda fuera de esta integración.

## Transacciones y eventos

La unidad raíz confirma las operaciones anidadas y solo después publica los eventos. Si revierte, descarta callbacks. La alternativa de notificar dentro de la transacción habría enviado avisos de operaciones que luego podrían fallar. La aceptación/cupo, pago/dos wallets y cancelación/reintegro conservan su unidad de trabajo.

Se reutiliza Observer sobre Booking y un adaptador BookingNotificationObserver lo conecta con el caso de uso de notificaciones; no hay imports de canales en dominio. La deduplicación usa un UUID estable para la transición y una restricción por evento/destinatario. Una excepción en un observer se registra y no corta al resto ni modifica el resultado del negocio confirmado.

El compromiso elegido es envío síncrono después del commit, sin outbox ni reintentos automáticos. No garantiza entrega si el proceso termina entre confirmación y envío. Esa limitación está explícita; agregar una cola durable sería una evolución separada.

## Presentación y errores

ApplicationControllers contiene interfaces y las consultas devuelven DTO, ControllerResult y Page. La pantalla formatea Instant en su zona horaria y presenta ErrorType. La identidad mock se valida en controllers; no se implementan credenciales, OAuth ni dinero real.

La bandeja y perfiles usan paginación SQL existente. La nueva consulta de wallet pagina el resultado del GetWalletService existente en memoria, por lo que todavía carga su ledger completo; se documenta y no se afirma resolver eficiencia SQL allí.

## Evidencia

FullJourneyIntegrationTest usa el root real con JPA, H2 y Flyway para publicar, contratar, pagar, reprogramar, completar y calificar, así como cancelar con configuración externa. JpaUnitOfWorkTest verifica commit/rollback raíz y callbacks anidados. El build exige 80 % de instrucciones en domain/application.

La conexión de pantallas está pendiente del kit de la cátedra. La validación final contra MySQL sigue siendo necesaria; H2 verifica el flujo y mapeo pero no reproduce todas las diferencias del motor.
