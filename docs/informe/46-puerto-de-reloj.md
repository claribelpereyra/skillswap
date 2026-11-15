# Caso 46: Puerto de reloj

## Objetivo

Permitir que los componentes que necesitan la fecha y hora actuales reciban el reloj por inyección. Así, las pruebas pueden usar fechas fijas y repetir los mismos escenarios temporales.

## Cambios realizados

- Se agregó `TimeProvider` en `application/ports/out`, con la operación `now()` que devuelve un `Instant`.
- Se agregó `SystemTimeProvider` en infraestructura. En producción usa un `Clock` UTC y también permite inyectar otro `Clock`.
- Los casos de uso de publicaciones que necesitan la hora actual reciben `TimeProvider` como dependencia.
- `Booking` recibe explícitamente el instante de creación y el de cada transición de estado; el dominio no consulta el reloj del sistema.
- Las pruebas pueden inyectar un proveedor fijo para controlar el tiempo de cada escenario.

## Resultado

El puerto permite probar de forma determinista reglas que dependen del tiempo, como la anticipación de una cancelación o la popularidad reciente, sin acoplar el dominio al reloj real.
