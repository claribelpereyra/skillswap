# Estado de tareas de Ian

Revisión de las tareas 88–107 del Excel del equipo. "Implementada" describe esta rama y sus pruebas; la aprobación/merge y la entrega académica se realizan aparte.

| # | Tarea | Estado y evidencia |
| --- | --- | --- |
| 88 | Historias de notificaciones y preferencias | Implementada: `88-historias-notificaciones.md` |
| 89 | Bosquejo de casos de uso y puertos | Actualizado para los módulos y contratos actuales; listo para trasladar a StarUML |
| 90 | Decisiones de arquitectura | Actualizadas con composición, callbacks y límites de persistencia |
| 91 | SOLID y GRASP | Evidencias actualizadas con clases y pruebas |
| 92 | Revisar Mock UI Kit | Implementada: ZIP completo inspeccionado; ver 105-integracion-mock-ui-kit.md |
| 93 | Evento y puerto de publicación | Implementada; incluye adaptación de eventos confirmados de contratación |
| 94 | Suscriptores de seis eventos | Conectados y probados en flujos reales; reutiliza la base Observer propuesta en #45 |
| 95 | Preferencia de canal | Persistencia y controller con identidad activa implementados |
| 96 | Simple Factory de canales | Conectada en el root con email, push e interna mock |
| 97 | Bandeja y lectura | Implementada y probada con permisos y paginación |
| 98 | Cobertura mínima | JaCoCo exige 80 % en domain y application mediante verify |
| 99 | Persistencia de notificaciones y preferencias | V9, JPA, H2/Flyway y recorridos reales sobre MySQL 8.4.13 |
| 100 | Adaptadores mock | Conectados por interfaces y usados desde eventos reales |
| 101 | Configuración externa | Aplicada a reintegros y estrategia predeterminada de recomendación |
| 102 | Errores uniformes | Controllers usan ControllerResult y ErrorType comunes |
| 103 | Controller de notificaciones | Renombrado a NotificationControllerImpl y ubicado en su paquete |
| 104 | Composition root | Main usa CompositionRoot; ApplicationRuntime administra cierre |
| 105 | Conectar kit | Implementada: RealUiControllers conecta Swing con los controllers reales |
| 106 | Modelos de vista | Modelos del kit mapeados a DTO reales; consultas y pantallas paginadas |
| 107 | Caso 8 y recorrido completo | Recorrido real validado sobre H2/MySQL y siete pantallas Swing verificadas |

## Cierre de las tareas de Ian

El kit ya está disponible e integrado; no queda pendiente pedirlo a la cátedra. La validación MySQL ya fue ejecutada y quedó automatizada en CI. Los avances de Observer de la #45 ya están incorporados en develop y se reutilizan sin otra implementación paralela.

Queda revisar e integrar esta rama con el equipo y actualizar el informe final y el UML de la entrega con los contratos agregados (CatalogController, adaptador de UI y proyecciones de listados). Ian indicó que el UML ya está hecho y que aportará el informe para actualizarlo al final; no se modificaron esos documentos externos.

La evidencia y los límites reales se describen en validacion-ui-mysql.md. No se implementa autenticación ni dinero real, ni se afirma durabilidad de avisos sin un outbox.
