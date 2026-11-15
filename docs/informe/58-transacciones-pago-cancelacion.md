# Caso 58: Transacciones de pago y cancelación

## Objetivo

Mantener el pago o la cancelación como una operación indivisible: un error no debe dejar movimientos, saldos o cambios persistidos solo de un lado.

## Cambios realizados

- `Wallet.payBookingTo` y `Wallet.refundBookingTo` validan ambas partes antes de confirmar los movimientos, y actualizan los dos historiales bajo un bloqueo compartido.
- `PayBookingService.pay` y `CancelBookingService.cancel` coordinan la contratación y ambas wallets dentro de `UnitOfWork`.
- La persistencia JPA guarda los cambios de las dos wallets en la misma transacción; si falla, la unidad de trabajo revierte ambas.
- Las pruebas de dominio cubren saldo insuficiente, desbordamiento, pago/reintegro duplicado y reintegro sin fondos. `JpaWalletRepositoryTest` verifica que el rollback de pago y cancelación no persista cambios parciales.

## Resultado

Los débitos y créditos pareados se confirman juntos, tanto en el dominio como al persistir las wallets.
