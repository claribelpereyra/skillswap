# Caso 55: Casos de uso de pago, cancelación y consulta de wallet

## Objetivo

Orquestar el pago y la cancelación de contrataciones, y permitir que cada usuario consulte su propia billetera mediante puertos simulados.

## Cambios realizados

- `PayBookingService.pay` busca una contratación aceptada, valida que pague el cliente, transfiere los SkillCoins con `BookingPaymentService.pay` y confirma el estado. Los cambios se ejecutan dentro de `UnitOfWork`.
- `CancelBookingService.cancel` cancela contrataciones aceptadas sin reintegro; si ya fueron pagadas, calcula el reintegro configurado y transfiere los fondos antes de guardar la cancelación.
- `GetWalletService.getWallet` devuelve el saldo y los movimientos de la billetera del usuario autenticado; no permite consultar la billetera de otra persona.
- Se agregaron los puertos de entrada para cada operación y los puertos de salida `BookingRepository` y `WalletRepository`.
- `WalletUseCasesTest` usa puertos simulados para verificar pagos, saldo insuficiente, cancelaciones, reintegros, consultas y control de acceso.

## Resultado

Las operaciones de aplicación coordinan el estado de la contratación y los movimientos de billetera, delegando reglas monetarias y de reintegro en el dominio.
