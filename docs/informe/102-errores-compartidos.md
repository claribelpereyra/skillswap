# Tarea 102 — Errores uniformes en controllers

Se adopta `ControllerResult<T>`, que ya usan contratación, publicaciones y búsqueda en develop. Se retiran `Resultado`, `ErrorController` y el traductor separado de notificaciones para que la interfaz no reciba dos formatos distintos.

El resultado comparte dato, tipo de error y mensaje. Clasifica permisos (de dominio o aplicación), reglas de negocio, recurso inexistente, concurrencia y datos inválidos. Los fallos inesperados usan un mensaje genérico, sin detalles de SQL ni credenciales. La excepción de permisos de dominio se evalúa antes que su clase base de reglas de negocio.

Notificaciones también recibe el puerto común `IdentityService`, en lugar de una función de sesión propia. Su excepción de recurso inexistente usa la jerarquía común y conserva el mismo resultado para un ID ajeno o inexistente.

Los tests comprueban todas las categorías y la ausencia de información técnica. Esta unificación está aplicada a los controllers presentes en develop y notificaciones. Perfiles y wallet deben conservar este contrato al mergear sus ramas; todavía no se incluyen en esta rama de pendientes.
