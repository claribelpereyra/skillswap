# Caso 46: Reloj inyectable

## Problema

Los casos de uso que registran fechas necesitan obtener la hora actual, pero las pruebas deben poder reproducir el mismo instante sin depender del reloj del sistema.

## Alternativa elegida

Se inyecta `java.time.Clock`, que ya ofrece `Clock.systemUTC()` para producción y `Clock.fixed(...)` para pruebas. No se agrega un puerto `TimeProvider` propio porque duplicaría esta abstracción sin aportar comportamiento adicional.

## Consecuencias

Los casos de uso reciben el reloj por constructor y las pruebas controlan la hora con un reloj fijo. El dominio de publicaciones no queda acoplado al reloj global; los cambios temporales se registran con el instante proporcionado por la aplicación.

## Evidencia

- `CreateDraftService` y `ChangePublicationStatusService` reciben `Clock`.
- `PublicationUseCasesTest` y `PublicationIntegrationTest` usan `Clock.fixed(...)` para comprobar el historial con un instante determinista.
