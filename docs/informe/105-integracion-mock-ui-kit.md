# Tareas 92 y 105 — Mock UI Kit conectado

Fuente: ZIP de la cátedra `0. TP_PDS_SkillSwap.zip` entregado por Ian. Se incorporaron los fuentes de `UiControllers`, `Models`, `SkillSwapView` y `Theme` dentro de app, manteniendo sus paquetes `ar.edu.uade.skillswap`. No se copiaron target, archivos de Eclipse/macOS ni el mock a producción. No se sustituyeron las versiones de Hibernate, Flyway o Java del proyecto por las del POM del ZIP.

SHA-256 del ZIP de referencia: `b3884b86e7850fd87647304a8bec37be41f4179ec36f042534d9f2b797bb2a03`.

`RealUiControllers` implementa el contrato de la cátedra y delega en `ApplicationControllers`. La pantalla no conoce repositorios, SQL ni entidades JPA. Main inicia CompositionRoot y abre el JFrame Swing con el adaptador real. Al cerrar la ventana se cierra el runtime.

## Ajustes necesarios al contrato del mock

- Identidad dinámica: selector de sesión simulada para Mateo/Sofía; ningún método cambia de actor silenciosamente. El backend valida los permisos.
- Solicitud con franja, mensaje y objetivo. El método simplificado del mock rechaza omitir esos datos.
- Crear borrador con tipo y horarios explícitos; publicación/pausa/cierre/cancelación por sus acciones reales.
- Pago, aceptación e inicio requieren el rol correspondiente; se agrega rechazo con motivo, cancelación con motivo y respuesta de reprogramación.
- Se conectan calificación verificada y comprobante de una contratación completada.
- Preferencia de email, push o interna; lectura individual o de todos los avisos del usuario activo.
- Anterior/Siguiente en búsqueda, publicaciones, contrataciones, movimientos y notificaciones. El listado combinado cliente/proveedor usa una página de cada rol y avanza ambos cursores juntos; no promete un orden global entre páginas de los dos roles.
- La búsqueda incorpora modalidad remota/presencial, precio máximo y orden. HIBRIDA se rechaza explícitamente: no existe en el dominio del enunciado.
- Los errores de ControllerResult se muestran como mensajes de aplicación. Las operaciones se ejecutan en SwingWorker fuera del EDT, con un diálogo modal de progreso mientras se conserva el contrato síncrono del kit.

La estética de Theme y la organización de las siete pantallas se conservan. No se usan nombres, saldos, reputación, avisos o contrataciones hardcodeados del mock. Las iniciales y la información del usuario provienen del perfil real.

La proyección de búsqueda incluye descripción, duración y nombre de proveedor en las tres consultas fijas existentes. Los títulos y nombres de la página de contrataciones se obtienen en una consulta agrupada por IDs; no se consulta un perfil/publicación por fila y el historial sigue visible al cerrarse una publicación.

La consulta individual usa `BookingController.get(userId, bookingId)` y `BookingService.get(bookingId, actor)`: comprueba que el actor sea participante y devuelve las etiquetas de esa reserva, sin recorrer los listados. Las respuestas de las acciones también incluyen etiquetas. Para dibujar los botones de reprogramación se usa el `proposedSlotId` del listado visible; las acciones releen siempre la reserva y validan sesión/permisos. `RealUiControllersBookingTest` cubre veinte filas sin consultas individuales, la relectura antes de actuar y la validación de sesión al usar el snapshot. `BookingLabelsQuery` es obligatorio en el único constructor del servicio; los tests que no necesitan etiquetas lo declaran explícitamente.

Desde la interfaz, una publicación nueva se crea con cupo 1 y sin etiquetas, porque el formulario del kit no los pide. El backend admite otros valores, y la demo y los tests los usan.

## Demostración

`--demo` crea de forma explícita los perfiles user-mateo/user-sofia, una wallet con 500 créditos iniciales por usuario y dos ofertas de mentoría con horarios. Usa controllers y datos persistidos; no vuelve a acreditar saldo ni duplica publicaciones al reiniciar. La elección de canales del usuario no se sobrescribe al reiniciar. No hay autenticación real.

Es la única fuente de datos de ejemplo. Se retiró el SQL de demo que no ejecutaba el arranque; las migraciones contienen solo esquema y los tests conservan fixtures aislados.

Ver README para empaquetado y ejecución. RealUiControllersIntegrationTest recorre los contratos del kit con persistencia real. SkillSwapViewIntegrationTest abre y pinta las siete pantallas con controllers reales en modo headless. SwingCallsTest verifica la ejecución fuera del EDT y la propagación del error de negocio.
