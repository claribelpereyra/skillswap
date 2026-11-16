# Caso 51: Pago de una contratación

## Objetivo

Debitar los SkillCoins del cliente y acreditarlos al proveedor como una sola operación, evitando que el mismo pago de contratación se procese más de una vez.

## Cambios realizados

- `BookingServiceImpl.pay` carga la contratación y ambas wallets en aplicación y coordina el pago dentro de una unidad de trabajo.
- `Wallet.payBookingTo` valida las dos billeteras, el monto, la fecha, los saldos y los movimientos previos antes de modificar cualquiera de ellas.
- El pago usa el precio final persistido en la contratación; el débito, el crédito y el cambio a `CONFIRMED` se confirman juntos.
- `JpaWalletRepository` guarda movimientos inmutables y usa `@Version`; un conflicto optimista se traduce a `ConcurrentUpdateException`.
- Se conserva el identificador de contratación en ambas billeteras. Una repetición del pago se rechaza sin generar otro débito o crédito.
- Las contrataciones gratuitas se confirman sin crear movimientos de monto cero.
- Si el saldo del cliente es insuficiente o el saldo del proveedor no puede incrementarse, ambas billeteras quedan sin cambios.

## Resultado

El dominio ofrece operaciones pareadas para pagar y reintegrar, sin bloqueo global ni movimientos unilaterales. La aplicación orquesta el pago con el importe persistido; JPA protege las escrituras concurrentes mediante versionado optimista.
