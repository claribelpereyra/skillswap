# Caso 55: Casos de uso de pago, cancelación y consulta de wallet

## Objetivo

Orquestar el pago y la cancelación de contrataciones, y permitir que cada usuario consulte su propia billetera mediante puertos simulados.

## Cambios realizados

- `BookingService.pay` confirma una contratación aceptada, aplica las promociones configuradas al precio guardado y transfiere el importe final mediante `BookingPaymentService`; la contratación y ambas wallets se guardan en una unidad de trabajo.
- `BookingService.cancel` cancela una contratación y libera su franja; si estaba confirmada, calcula el reintegro sobre el importe realmente cobrado antes de guardar la operación.
- `GetWalletService.getWallet` devuelve el saldo y los movimientos de la billetera del usuario autenticado; no permite consultar la billetera de otra persona.
- Se agregaron los puertos de entrada para cada operación y los puertos de salida `BookingRepository` y `WalletRepository`.
- Los tests de casos de uso verifican transferencia con promociones, saldo insuficiente, liberación de franja, reintegro basado en el pago real, consulta y control de acceso.

## Resultado

Las operaciones de aplicación coordinan el estado de la contratación y los movimientos de billetera, delegando reglas monetarias y de reintegro en el dominio.
