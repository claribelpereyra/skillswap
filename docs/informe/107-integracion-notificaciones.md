# Tarea 107 — Integración de notificaciones y recorrido completo

`FullJourneyIntegrationTest` abre el mismo `CompositionRoot` usado por Main, con H2 en modo MySQL, las migraciones Flyway reales y Hibernate validate. Solo fija el reloj y simula el login y los canales externos; publicaciones, perfiles, contratación, wallets, calificaciones, preferencias y bandeja usan persistencia real.

El recorrido crea perfiles y wallets, configura email y push, publica una mentoría, busca y recomienda, solicita, acepta, reprograma con ambas partes, paga, inicia, completa y califica. Comprueba reputación, comprobante, saldos, rechazo de doble pago y de doble calificación, permisos y los cinco avisos del recorrido para cada participante.

Otro recorrido paga y cancela con umbrales externos 240/24, verifica reintegro del 50 %, liberación del cupo y el sexto evento. Se comprueba paginación de movimientos, lectura idempotente y que un usuario no pueda leer el aviso de otro.

Una transacción externa que revierte una solicitud no persiste contratación ni notificaciones. Las pruebas de `JpaUnitOfWork` verifican que los callbacks anidados se ejecutan una sola vez después del commit raíz y se eliminan al revertir.

`NotificationIntegrationTest` conserva además la prueba de deduplicación al reprocesar un evento idéntico. Los fallos de observers y callbacks se registran, sin reintentos automáticos ni garantía de entrega durable.

Esto verifica la integración de backend. Faltan la demostración sobre el Mock UI Kit y la verificación contra MySQL real; H2 no sustituye esa validación final.
