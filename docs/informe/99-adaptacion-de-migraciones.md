# Tarea 99 — Compatibilidad con la integración del equipo

La migración de notificaciones pasa a `V9__notificaciones_y_preferencias.sql`. La V2 ya está ocupada en develop por bookings y las PR de perfiles y wallets proponen ambas V5. Se deja V9 para no interferir con la numeración que acuerden sus responsables. No se altera una migración ya mergeada en develop.

La numeración salta de V6 a V9. Flyway rechaza una V7 o V8 agregada después en una base que ya aplicó V9, así que la próxima migración es `V10`.

Los enums de las entidades de notificación declaran almacenamiento JDBC VARCHAR de forma explícita para coincidir con las columnas de la migración, también cuando Hibernate usa el dialecto MySQL. Las fechas se almacenan en UTC con precisión de microsegundos.

Después de renombrar migraciones hay que usar `mvn clean verify`: `verify` sin clean puede conservar versiones anteriores en `target/classes` y Flyway las interpreta como migraciones adicionales. Las bases persistentes que ya hayan aplicado una versión anterior deben revisarse antes de actualizar; no se borra ni reescribe su historial automáticamente.
