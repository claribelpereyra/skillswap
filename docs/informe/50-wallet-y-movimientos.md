# Caso 49: Wallet y movimientos de créditos

## Objetivo

Representar el saldo de SkillCoins de una persona y registrar los movimientos que lo modifican, manteniendo el saldo sin valores negativos.

## Cambios realizados

- Se agregó `Wallet`, asociado a un `UserId`, que comienza con saldo cero.
- La billetera permite registrar cargas (`TOP_UP`), débitos (`DEBIT`), créditos (`CREDIT`) y reintegros (`REFUND`).
- Cada `CreditMovement` registra tipo, monto positivo, instante de ocurrencia y, cuando corresponde, la contratación asociada.
- Las cargas no se vinculan a una contratación; los débitos, créditos y reintegros requieren un identificador de contratación positivo.
- Los movimientos se registran en orden cronológico. Un débito que supera el saldo disponible es rechazado y el historial se expone como una copia inmutable.

## Resultado

El saldo se calcula aplicando cada movimiento mediante `SkillCoins`, que impide saldos negativos y rechaza débitos insuficientes. El cambio cubre el modelo de dominio; no agrega persistencia ni casos de uso para la billetera.
