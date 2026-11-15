# Caso 52: Políticas de reintegro con Strategy

## Objetivo

Aplicar un reintegro del 100 %, 50 % o 0 % según la anticipación con la que se cancela una contratación, permitiendo configurar los umbrales.

## Cambios realizados

- Se definieron `RefundStrategy`, `TieredRefundStrategy` y `FullRefundStrategy` para poder intercambiar políticas con resultados distintos.
- `RefundPolicy` recibe los umbrales de reintegro completo y parcial como duraciones configurables, y valida que sean no negativos y estén ordenados.
- Con al menos el umbral completo se reintegra el 100 %; con al menos el umbral parcial se reintegra el 50 %; con menor anticipación se reintegra el 0 %.
- Para un monto impar, el reintegro del 50 % se redondea hacia abajo a SkillCoins enteros.
- El reintegro mueve créditos del proveedor al cliente, queda asociado a la contratación y no puede aplicarse dos veces.
- Un reintegro de 0 % no crea movimientos de monto cero; el caso de uso de cancelación impide repetir la transición de la contratación.

## Resultado

La estrategia puede reemplazarse sin cambiar el caso de uso consumidor. Los tests calculan montos distintos para la misma reserva usando `TieredRefundStrategy` y `FullRefundStrategy`; los umbrales se suministran al construir la política.
