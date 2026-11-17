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

La bandeja y perfiles usan paginación SQL existente. La consulta de wallet también pagina en SQL: `WalletStatementQuery` lee el saldo de la fila de la wallet, cuenta los movimientos y trae solo la página pedida. El pago sigue cargando el agregado completo, porque es el que protege el doble cobro en el dominio.

## Evidencia

FullJourneyIntegrationTest usa el root real con JPA, H2 y Flyway para publicar, contratar, pagar, reprogramar, completar y calificar, así como cancelar con configuración externa. JpaUnitOfWorkTest verifica commit/rollback raíz y callbacks anidados. El build exige 80 % de instrucciones en domain/application.

La pantalla Swing de la cátedra ya está conectada por RealUiControllers. Hay validación adicional de los recorridos completos y del adaptador sobre MySQL 8.4.13, además de H2. Ver validacion-ui-mysql.md.
