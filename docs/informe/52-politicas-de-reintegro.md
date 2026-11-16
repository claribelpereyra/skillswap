# Caso 52: Políticas de reintegro con Strategy

## Objetivo

Aplicar un reintegro del 100 %, 50 % o 0 % según la anticipación con la que se cancela una contratación, permitiendo configurar los umbrales.

## Cambios realizados

- Se definieron `RefundStrategy`, `TieredRefundStrategy` y `FullRefundStrategy` para poder intercambiar políticas con resultados distintos.
- `RefundPolicy` recibe los umbrales de reintegro completo y parcial como duraciones configurables, y valida que sean no negativos y estén ordenados.
- Con al menos el umbral completo se reintegra el 100 %; con al menos el umbral parcial se reintegra el 50 %; con menor anticipación se reintegra el 0 %.
- Para un monto impar, el reintegro del 50 % se redondea hacia abajo a SkillCoins enteros.
- El reintegro se calcula sobre el precio final persistido en la contratación, mueve créditos del proveedor al cliente y queda asociado a la contratación.
- Un reintegro de 0 % no crea movimientos de monto cero; el caso de uso de cancelación impide repetir la transición de la contratación.
- Decisión: no se retienen SkillCoins en escrow. Si el proveedor ya gastó el saldo que debe reintegrarse, la cancelación falla dentro de la unidad de trabajo y la contratación conserva su estado y cupo; no se permiten saldos negativos ni se emite crédito.

## Resultado

La estrategia puede reemplazarse sin cambiar el caso de uso consumidor. Un test ejecuta la misma cancelación con `TieredRefundStrategy` y `FullRefundStrategy` y verifica reintegros distintos. La política de no-escrow mantiene las wallets consistentes, aunque puede impedir cancelar hasta que el proveedor tenga saldo suficiente.
