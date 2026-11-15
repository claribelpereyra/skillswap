# Caso 63: Controllers de contratación y wallet

## Objetivo

Exponer a la UI las acciones de pago y cancelación de una contratación y la consulta del saldo y los movimientos del usuario activo.

## Cambios realizados

- `BookingController.pay` y `BookingController.cancel` exponen el pago y la cancelación desde el puerto existente de contratación.
- `WalletController.getMyWallet` consulta la wallet del usuario autenticado; `loadCredits` permite solicitar una carga simulada.
- Los controllers verifican la identidad con `IdentityService` y devuelven errores mediante `ControllerResult`.
- Las pruebas unitarias verifican delegación, identidad activa y rechazo de entradas inválidas.

## Resultado

La UI puede pagar, cancelar, consultar su wallet y solicitar una carga mediante controllers delgados, sin acceder directamente a reglas de negocio ni repositorios.
