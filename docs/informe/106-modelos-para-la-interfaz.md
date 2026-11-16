# Tarea 106 — Modelos de vista

Se prepara `NotificationView` para la bandeja: ID, tipo, referencia, mensaje, fecha, canal, resultado del envío y estado/fecha de lectura. No expone entidades JPA ni el destinatario como un parámetro que permita consultar otro usuario. Los instantes se conservan como `Instant`; la futura pantalla debe mostrarlos en la zona horaria correspondiente, sin formatear fechas dentro de dominio.

El contrato `controllers.notification.NotificationController` devuelve `ControllerResult<Page<NotificationView>>`, con los mismos tipos de resultado y paginación comunes que publicaciones y búsqueda. La implementación transforma el modelo de notificaciones antes de devolverlo. La UI puede conservar la interfaz y reemplazar un mock por la implementación sin acceder a repositorios.

En develop ya están disponibles `PublicationView`, `PublicationSummary`, `BookingView` y `BookingHistoryEntry`. Los modelos de perfil, wallet y calificaciones siguen en las PR de sus responsables; deben revisarse después del merge. No se duplican sus DTO en esta tarea.

Este avance prepara la bandeja y el contrato de integración. La tarea completa requiere contrastar los datos de cada pantalla con el Mock UI Kit de la cátedra, que no se encuentra en el checkout revisado. No se inventan firmas de un kit que todavía no está disponible ni se presenta como conectada una pantalla inexistente.
