# Caso 58: Transacciones de pago y cancelación

## Objetivo

Mantener el pago o la cancelación como una operación indivisible: un error no debe dejar movimientos, saldos o cambios persistidos solo de un lado.

## Cambios realizados

- `Wallet.payBookingTo` y `Wallet.refundBookingTo` validan ambas partes antes de confirmar los movimientos pareados; no usan un bloqueo global.
- `BookingServiceImpl.pay` y `BookingServiceImpl.cancel` coordinan la contratación, ambas wallets y la disponibilidad dentro de `UnitOfWork`.
- La persistencia JPA guarda los cambios de las dos wallets en la misma transacción; si falla, la unidad de trabajo revierte ambas.
- `@Version` y el bloqueo optimista de JPA detectan escrituras concurrentes; una operación obsoleta falla en vez de sobrescribir el saldo.
- Las pruebas de dominio cubren saldo insuficiente, desbordamiento, pago/reintegro duplicado y reintegro sin fondos. `JpaWalletRepositoryTest` verifica rollback y concurrencia sin movimientos parciales.

## Resultado

Los débitos y créditos pareados se confirman juntos, tanto en el dominio como al persistir las wallets.
