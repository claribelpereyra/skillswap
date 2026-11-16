# Tarea 91 — Evidencia de SOLID y GRASP

Se describen implementaciones actuales, no solo interfaces propuestas.

| Principio | Evidencia | Prueba o consecuencia |
| --- | --- | --- |
| SRP | Booking valida transiciones; BookingServiceImpl coordina; JpaBookingRepository persiste; BookingControllerImpl valida sesión | Reglas de dominio y tests de controller/persistencia separados |
| OCP | RecommendationStrategy y CanalNotificacion permiten variantes | Mismo caso de uso con afinidad/popularidad; fábrica con email/push/interna |
| LSP | Canales implementan el mismo contrato de aviso; estrategias conservan la colección de candidatos | Tests de canales y estrategias, sin exigir datos fuera del contrato |
| ISP | Puertos de perfil, calificación, búsqueda, preferencias y bandeja separados | Un controller de consulta no necesita métodos de escritura de repositorios |
| DIP | Casos de uso dependen de puertos y UnitOfWork; infraestructura los implementa | Tests con puertos simulados y el mismo código con repositorios JPA |

| GRASP | Implementación concreta |
| --- | --- |
| Information Expert | Wallet conoce su ledger y saldo; Profile mantiene suma/cantidad de calificaciones; Booking conoce estado y participantes |
| Controller | Interfaces de controllers y ControllerResult, sin SQL ni componentes visuales |
| Creator | CompositionRoot crea y conecta el grafo de producción; ApplicationRuntime conserva su ciclo de vida |
| Low Coupling / High Cohesion | UI recibe ApplicationControllers; adaptadores JPA no se exponen a pantallas |
| Polymorphism | BookingState, RecommendationStrategy, RefundStrategy, CanalNotificacion y BookingObserver |
| Indirection / Protected Variations | BookingNotificationObserver adapta eventos a avisos sin acoplar Booking a los canales; IdentityService y SkillCoinGateway aíslan los mocks |

Patrones con variación real: State para transiciones, Strategy para recomendación/reintegro, Observer para avisos, Adapter para identidad/pasarela/canales, Simple Factory para canal, Facade para perfil público, Decorator para promociones y Composite para catálogo. La existencia de implements no basta: el recorrido real FullJourneyIntegrationTest comprueba colaboración entre las piezas y persistencia.

Tradeoffs explícitos: la fábrica y el root deben registrar nuevas variantes; el contexto JPA único limita aislamiento de aplicaciones en la JVM; el envío después del commit protege el resultado del negocio pero no es durable. No se presentan estos compromisos como garantías de producción.

Evidencia adicional: RecommendationControllerTest demuestra el default configurable sin cambiar el caso de uso; BookingObserverTest registra e aísla errores; JpaUnitOfWorkTest prueba callbacks anidados y rollback; FullJourneyIntegrationTest comprueba permisos, pagos, cancelación, reputación, dos canales y paginación.
