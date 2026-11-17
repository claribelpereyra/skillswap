# Tarea 103 — Controller de notificaciones

`controllers.notification.NotificationControllerImpl` implementa `NotificationController`. Recibe la entrada de bandeja e IdentityService; consulta el usuario activo en cada operación y no permite elegir otro destinatario desde la pantalla.

`listar(page,size)` devuelve `ControllerResult<Page<NotificationView>>`. `marcarLeida(id)` valida el identificador y el caso de uso/repositorio comprueban la pertenencia e idempotencia. Los errores usan ErrorType común: INVALID_INPUT, ACCESS_DENIED, NOT_FOUND, BUSINESS_RULE, CONFLICT y UNEXPECTED.

`NotificationPreferencesControllerImpl` permite consultar y cambiar EMAIL/PUSH/INTERNA para el usuario de la misma sesión. No recibe un userId arbitrario. La fábrica y los adaptadores se registran en CompositionRoot.

La migración de notificaciones es V9 y los paquetes están en los cinco módulos Maven actuales. Los avisos de todos los canales se guardan en la bandeja; persistencia deduplica por evento/destinatario. El envío y el registro de resultado no son atómicos: una caída puede dejar PENDIENTE. Los fallos se registran; no hay reintentos automáticos ni garantía de entrega durable.

NotificationIntegrationTest verifica deduplicación, dos canales y lectura. FullJourneyIntegrationTest verifica además eventos originados por operaciones reales, preferencias, permisos y rollback. Estos contratos están conectados al Mock UI Kit Swing y los recorridos reales se validaron sobre MySQL 8.4.13. Ver validacion-ui-mysql.md.
