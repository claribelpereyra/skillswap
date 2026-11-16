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

Los modelos ya se contrastaron con el ZIP del kit: RealUiControllers mapea los DTO a Models y la pantalla Swing está conectada. PublicationSummary incluye descripción/duración/nombre por proyección; BookingView incluye títulos y nombres cargados por lote. Ver 105-integracion-mock-ui-kit.md y validacion-ui-mysql.md.
