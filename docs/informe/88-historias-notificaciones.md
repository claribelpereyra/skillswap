# Tarea 88 — Historias de notificaciones y preferencias

Fuente: consigna SkillSwap, secciones 4.6, 9, 10 y caso de aceptación 8.
Alcance: historias y criterios de aceptación; no implica que los flujos estén implementados.

## HU-N01 — Solicitud recibida

Como proveedor, quiero recibir una notificación cuando un cliente solicita mi publicación, para evaluar su solicitud.

- Dada una solicitud válida registrada, cuando se confirma la operación, se notifica al proveedor.
- La notificación identifica la solicitud, la publicación y al cliente.
- Una solicitud rechazada por las reglas del dominio no genera una notificación de solicitud recibida.

## HU-N02 — Aceptación

Como cliente, quiero saber que el proveedor aceptó mi solicitud, para continuar con el pago de la reserva.

- Cuando la aceptación y la reserva de cupo se confirman, se notifica al cliente.
- La notificación referencia la contratación y la franja reservada.
- Si la aceptación falla, por ejemplo por cupo agotado, no se anuncia una reserva exitosa.

## HU-N03 — Pago confirmado

Como participante de una contratación, quiero recibir la confirmación de pago, para conocer que la reserva quedó confirmada.

- Después de confirmar el débito y el crédito de SkillCoins, se notifica a cliente y proveedor.
- El mensaje identifica la contratación y el importe confirmado.
- Un pago rechazado por saldo insuficiente no genera una confirmación.
- El intento de pagar nuevamente una contratación ya pagada no genera una nueva confirmación de pago.

## HU-N04 — Reprogramación

Como participante, quiero conocer la nueva franja acordada, para asistir en el horario correcto.

- Cuando ambas partes aceptan una reprogramación válida y se confirma el cambio, se notifica a ambas.
- El mensaje identifica la contratación y las franjas anterior y nueva.
- Una propuesta pendiente o rechazada no se presenta como una reprogramación confirmada.

## HU-N05 — Cancelación

Como participante, quiero enterarme de una cancelación, para conocer su motivo y el resultado económico.

- Cuando se confirma una cancelación válida, se notifica a ambas partes.
- El mensaje referencia la contratación, el motivo y el reintegro calculado por la política aplicable, incluso si es cero.
- Las notificaciones no calculan el reintegro: reciben el resultado del caso de uso de cancelación.
- Una cancelación rechazada no se anuncia como realizada.

## HU-N06 — Sesión completada

Como participante, quiero saber que la sesión se completó, para poder calificar a la otra parte.

- Al confirmar la transición a COMPLETADA, se notifica a ambas partes.
- El mensaje referencia la contratación e informa que se habilita la calificación.
- La notificación no autoriza por sí sola a calificar: el caso de uso correspondiente valida las reglas.

## HU-N07 — Preferencia de canal

Como usuario, quiero elegir email, push o notificación interna, para recibir los avisos por mi canal preferido.

- El usuario puede consultar y actualizar su propia preferencia mediante su userId de identidad mock.
- Se acepta exactamente uno de los tres canales por usuario; se rechazan canales desconocidos.
- Los eventos posteriores a un cambio usan la preferencia vigente al procesar la notificación.
- Los envíos realizados conservan el canal con el que fueron procesados.
- Los proveedores de email y push son simulados y reemplazables mediante interfaces; no se requieren credenciales reales.

## HU-N08 — Bandeja y lectura

Como usuario, quiero consultar mis notificaciones y marcar una como leída, para distinguir los avisos pendientes.

- La consulta devuelve únicamente notificaciones del usuario activo y admite paginación.
- Marcar una notificación propia como leída conserva su contenido y registra la lectura.
- Repetir la marca de lectura no crea otra notificación.
- No se permite consultar ni modificar notificaciones ajenas.

## Matriz de eventos y destinatarios

| Evento confirmado | Destinatarios | Información específica |
| --- | --- | --- |
| Solicitud recibida | Proveedor | Solicitud, publicación y cliente |
| Aceptación | Cliente | Contratación y franja reservada |
| Pago confirmado | Cliente y proveedor | Contratación e importe |
| Reprogramación | Cliente y proveedor | Contratación, franja anterior y nueva |
| Cancelación | Cliente y proveedor | Contratación, motivo y reintegro |
| Sesión completada | Cliente y proveedor | Contratación y habilitación para calificar |

## Criterio de aceptación 8 — Dos canales para un evento

Dado un cliente con preferencia email y un proveedor con preferencia push, cuando se confirma un pago de una contratación entre ambos, el mismo evento produce un envío al cliente mediante el adaptador mock de email y otro al proveedor mediante el adaptador mock de push. Cada envío conserva su destinatario y referencia a la contratación. Esto permite verificar dos tipos de notificación sin exigir múltiples canales simultáneos para un mismo usuario.

## Decisiones propuestas para implementar

Estas precisiones completan aspectos que la consigna no define; deben mantenerse consistentes al implementar los casos de uso.

- Usar canal interno como valor inicial cuando el usuario todavía no eligió uno.
- Conservar en la bandeja un registro de cada aviso, cualquiera sea el canal de entrega elegido, para permitir su consulta y lectura.
- Ordenar la bandeja por fecha descendente y por identificador como desempate estable.
- Incluir identificador de evento, tipo, fecha, destinatario y referencia de negocio para trazabilidad. Evitar duplicados por la combinación evento y destinatario.
- Publicar eventos exitosos después de confirmar la operación de negocio. La coordinación con la transacción y el tratamiento de fallos de entrega se resolverán en las tareas de aplicación y persistencia; estas historias no prometen entrega garantizada.

## Dependencias y límites

- Las tareas 93 a 100 implementarán eventos, Observer, preferencias, fábrica, bandeja, persistencia y adaptadores.
- El equipo debe emitir los seis eventos desde los casos de uso reales; las pruebas aisladas con eventos simulados no sustituyen esa integración.
- Las tareas 103, 105 y 107 conectaron controller, UI y recorrido integrado; el Mock UI Kit está integrado en el módulo `app` (ver doc 105).
- No se incorpora autenticación real, envío a proveedores reales ni rediseño visual.
