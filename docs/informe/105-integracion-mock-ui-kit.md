# Tareas 92 y 105 — Contrato para conectar el Mock UI Kit

El kit Swing/JavaFX de la cátedra no está en el repositorio revisado. No se sustituyen sus pantallas ni se inventa una clase `MockSkillSwapController`. Falta recibir su código o JAR y su punto de entrada para verificar firmas y realizar la conexión final.

El backend está preparado: `CompositionRoot.open(...)` devuelve un `ApplicationRuntime`; `runtime.controllers()` contiene interfaces y se pasa a un adaptador del kit. El runtime debe permanecer abierto durante la vida de las ventanas y cerrarse al salir. No se debe abrir una factory por pantalla.

| Pantalla | Interfaz disponible | Datos / operaciones |
| --- | --- | --- |
| Login simulado | `SessionController` | `signIn`, `signOut`, `currentUserId`; usuario compartido entre controllers |
| Inicio y búsqueda | `SearchController`, `RecommendationController` | `Page<PublicationSummary>` y formularios de filtros |
| Detalle y panel proveedor | `PublicationController`, `BookingController` | Publicación, franjas, solicitud, aceptación, pago, reprogramación y estados |
| Wallet | `WalletController`, `WalletQueryController` | Cargar créditos; consultar saldo y `Page<WalletMovementView>` |
| Comprobante | `BookingReceiptController` | Consulta de una contratación completada, solo por participantes |
| Perfil y calificaciones | `ProfileController`, `RatingController` | Perfil público, reputación, publicaciones y calificaciones paginadas |
| Notificaciones | `NotificationController`, `NotificationPreferencesController` | Bandeja, lectura y consulta/cambio del canal del usuario activo |

Todos los métodos con resultado devuelven `ControllerResult<T>`. La pantalla diferencia `INVALID_INPUT`, `ACCESS_DENIED`, `NOT_FOUND`, `CONFLICT`, `BUSINESS_RULE` y `UNEXPECTED`; no muestra excepciones técnicas. Los DTO conservan `Instant` para que la UI aplique la zona horaria del perfil. La selección de identidad mock es de demostración, no un control de acceso real para producción.

El contrato `NotificationController` conserva sus operaciones `listar` y `marcarLeida`; su implementación se llama `NotificationControllerImpl` y está en `controllers.notification`, según la observación de revisión.

Al recibir el kit: identificar sus interfaces; implementar un adaptador que traduzca nombres/formularios/DTO; conectar su arranque desde Main; probar búsquedas, errores, paginación, lectura, login y el recorrido completo sin rediseñar ventanas. Las llamadas JPA y los envíos síncronos deben ejecutarse fuera del hilo de eventos Swing/JavaFX para no bloquear la interfaz.
