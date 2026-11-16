# Estado de tareas de Ian

Revisión de las tareas 88–107 del Excel del equipo. "Implementada" describe esta rama y sus pruebas; la aprobación/merge y la entrega académica se realizan aparte.

| # | Tarea | Estado y evidencia |
| --- | --- | --- |
| 88 | Historias de notificaciones y preferencias | Implementada: `88-historias-notificaciones.md` |
| 89 | Bosquejo de casos de uso y puertos | Actualizado para los módulos y contratos actuales; listo para trasladar a StarUML |
| 90 | Decisiones de arquitectura | Actualizadas con composición, callbacks y límites de persistencia |
| 91 | SOLID y GRASP | Evidencias actualizadas con clases y pruebas |
| 92 | Revisar Mock UI Kit | Bloqueada por falta del kit de la cátedra |
| 93 | Evento y puerto de publicación | Implementada; incluye adaptación de eventos confirmados de contratación |
| 94 | Suscriptores de seis eventos | Conectados y probados en flujos reales; reutiliza la base Observer propuesta en #45 |
| 95 | Preferencia de canal | Persistencia y controller con identidad activa implementados |
| 96 | Simple Factory de canales | Conectada en el root con email, push e interna mock |
| 97 | Bandeja y lectura | Implementada y probada con permisos y paginación |
| 98 | Cobertura mínima | JaCoCo exige 80 % en domain y application mediante verify |
| 99 | Persistencia de notificaciones y preferencias | V9, JPA, H2 y Flyway; falta validar MySQL real |
| 100 | Adaptadores mock | Conectados por interfaces y usados desde eventos reales |
| 101 | Configuración externa | Aplicada a reintegros y estrategia predeterminada de recomendación |
| 102 | Errores uniformes | Controllers usan ControllerResult y ErrorType comunes |
| 103 | Controller de notificaciones | Renombrado a NotificationControllerImpl y ubicado en su paquete |
| 104 | Composition root | Main usa CompositionRoot; ApplicationRuntime administra cierre |
| 105 | Conectar kit | Contratos preparados; conexión final bloqueada por falta del kit |
| 106 | Modelos de vista | Backend preparado, incluido saldo y movimientos paginados; falta contrastar pantallas del kit |
| 107 | Caso 8 y recorrido completo | Backend integrado con controllers y persistencia reales; demo visual pendiente del kit |

## Lo que debe aportar o coordinar el equipo

1. Entregar el Mock UI Kit (código/JAR, interfaces y clase de arranque) para completar 92/105 y revisar el ajuste visual de 106/107.
2. Coordinar esta rama con la PR #45: se reutilizan sus contratos BookingObserver/BookingEvent y afterCommit, junto con correcciones de pago/cancelación, logs y pruebas reales. No integrar dos versiones distintas de la misma base.
3. Ejecutar una validación final contra MySQL con Docker y revisar el UML compartido de Luqui usando los contratos actuales.

El informe describe límites reales: envío síncrono después del commit, sin outbox ni reintento automático; una sola factory por proceso; paginación de movimientos en memoria sobre el ledger existente. No se implementa autenticación ni dinero real.
