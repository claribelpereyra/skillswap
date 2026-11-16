# Caso 51: Pago de una contratación

## Objetivo

Debitar los SkillCoins del cliente y acreditarlos al proveedor como una sola operación, evitando que el mismo pago de contratación se procese más de una vez.

## Cambios realizados

- Se agregó `BookingPaymentService` para procesar pagos asociados a un identificador de contratación.
- `Wallet.payBookingTo` valida las dos billeteras, el monto, la fecha, los saldos y los movimientos previos antes de modificar cualquiera de ellas.
- El débito del cliente y el crédito del proveedor se preparan antes de guardar ambos movimientos; la unidad de trabajo de aplicación aporta la atomicidad persistente.
- Se conserva el identificador de contratación en ambas billeteras. Una repetición del pago se rechaza sin generar otro débito o crédito.
- Si el saldo del cliente es insuficiente o el saldo del proveedor no puede incrementarse, ambas billeteras quedan sin cambios.

## Resultado

El dominio ofrece solo operaciones pareadas para pagar y reintegrar; no mantiene un bloqueo global ni permite débitos o créditos sueltos. `BookingServiceImpl` carga la contratación y ambas wallets, usa el precio persistido de la reserva y confirma el estado dentro de la misma unidad de trabajo. Las promociones configuradas determinan el importe final al cobrar.
