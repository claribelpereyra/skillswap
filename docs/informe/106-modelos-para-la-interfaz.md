# Tarea 106 — Modelos de vista

`ApplicationControllers` agrupa interfaces disponibles para la presentación, sin acceso a entidades JPA ni repositorios.

| Consulta | Modelo |
| --- | --- |
| Búsqueda y recomendación | `Page<PublicationSummary>` |
| Detalle y publicaciones propias | `PublicationView` / `Page<PublicationView>` |
| Contrataciones | `BookingView`, `Page<BookingView>` e historial `BookingHistoryEntry` |
| Perfil público | `PublicProfileView`, con publicaciones y calificaciones paginadas |
| Calificaciones | `RatingView` / `Page<RatingView>` |
| Wallet | `WalletSummaryView`: saldo y `Page<WalletMovementView>` |
| Comprobante | `BookingReceipt` |
| Bandeja | `Page<NotificationView>`, con canal, estado de entrega y lectura |

`WalletQueryController.getMyWallet(page,size)` obtiene el dueño de la identidad activa, valida la página y recorta movimientos antes de devolverlos. La consulta reutiliza `GetWalletService`; actualmente carga el ledger completo en el backend y no promete paginación SQL. El método sin argumentos usa página 0 y tamaño 20.

Las vistas mantienen fechas como `Instant`; el formateo y la zona horaria pertenecen a la presentación. Cada operación usa `ControllerResult<T>` para compartir el formato de error.

Los modelos de backend están disponibles y probados; falta contrastarlos contra los campos efectivos del Mock UI Kit cuando se entregue. No se afirma que las pantallas estén conectadas.
