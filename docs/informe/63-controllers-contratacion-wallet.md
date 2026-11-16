# Caso 63: Controllers de contratación y wallet

## Objetivo

Exponer a la UI las acciones de pago y cancelación de una contratación y la consulta del saldo y los movimientos del usuario activo.

## Cambios realizados

- `BookingController.pay` y `BookingController.cancel` exponen el pago y la cancelación desde el puerto existente de contratación.
- `WalletController.getMyWallet` consulta la wallet del usuario autenticado; `loadCredits` permite solicitar una carga simulada.
- Los controllers comparan el `userId` recibido con `IdentityService.currentUserId()` antes de delegar acciones de escritura o consulta.
- Los casos de uso y el dominio vuelven a validar permisos de cliente, participante y propietario; el controller no reemplaza esas reglas.
- Los resultados se devuelven mediante `ControllerResult`, con errores de entrada, identidad y negocio en formato uniforme.
- Las pruebas unitarias verifican delegación, identidad activa y rechazo de entradas inválidas.

## Resultado

La UI puede pagar, cancelar y consultar su wallet mediante controllers delgados, sin acceder directamente a reglas de negocio ni repositorios.
