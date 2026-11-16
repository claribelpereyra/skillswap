# Caso 50: Wallet y movimientos de créditos

## Problema

SkillSwap necesita representar el saldo de SkillCoins de cada persona y conservar un historial ordenado de las operaciones que lo modifican, sin permitir saldos negativos.

## Alternativa elegida

Se modela una `Wallet` de dominio con movimientos `TOP_UP`, `DEBIT`, `CREDIT` y `REFUND`. El saldo se actualiza a partir de cada movimiento y `Wallet.restore(...)` reconstruye el estado aplicando el ledger en orden, en vez de aceptar un saldo independiente que pudiera quedar desfasado.

## Consecuencias

- Cada movimiento tiene un importe positivo, un instante y, salvo las cargas, un identificador de contratación.
- Los movimientos fuera de orden se rechazan con `IllegalStateException`.
- Un débito superior al saldo se rechaza con `InsufficientBalanceException`, sin cambiar el saldo ni el ledger.
- El historial expuesto es inmutable.

## Evidencia

`WalletTest` verifica la carga, los movimientos asociados, la restauración desde el ledger, el rechazo de saldo insuficiente y el rechazo de movimientos retroactivos.
