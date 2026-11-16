# Tarea 107 — Integración

Se agrega `NotificationIntegrationTest` en app. El caso 8 recorre publicador Observer, suscriptor, caso de uso, preferencias persistidas, fábrica, adaptadores mock, repositorio JPA, controller y DTO de bandeja. Usa H2 con las migraciones reales de Flyway y Hibernate en modo validate.

Un pago confirmado simulado genera un email al cliente y un push al proveedor. Reprocesar el evento no duplica avisos; el controller lista solo la bandeja activa, rechaza marcar avisos ajenos y conserva la fecha de primera lectura. Sin sesión, devuelve ACCESS_DENIED.

El evento se construye en el test: todavía no prueba que el pago real lo emita. Para completar la tarea falta el recorrido publicar → solicitar → aceptar → pagar → completar → calificar con las PR de perfiles, pagos y Observer mergeadas. Esa integración no se incluye hasta que el equipo incorpore sus ramas a develop.
