# Caso 55: Casos de uso de pago, cancelación y consulta de wallet

## Objetivo

Orquestar el pago y la cancelación de contrataciones, y permitir que cada usuario consulte su propia billetera mediante puertos simulados.

## Cambios realizados

- `BookingService.pay` confirma el estado aceptado una sola vez, transfiere los SkillCoins mediante `BookingPaymentService` y guarda la contratación y ambas wallets en una unidad de trabajo.
- `BookingService.cancel` cancela una contratación y libera su franja; si estaba confirmada, reintegra el importe configurado antes de guardar la operación.
- `GetWalletService` devuelve un DTO simple de saldo y movimientos, y solo permite consultar la wallet del usuario autenticado.
- Pago y cancelación se exponen desde el puerto `BookingService` y `BookingController`; se reutiliza el `BookingRepository` existente.
- Los comandos y vistas de booking y wallet están en `application.dto.booking` y `application.dto.wallet`; las vistas no exponen entidades ni tipos de dominio.
- Los tests verifican transferencia, saldo insuficiente, liberación de franja, reintegro, consulta y control de acceso.

## Resultado

Las operaciones de aplicación coordinan el estado de la contratación y los movimientos de billetera, delegando reglas monetarias y de reintegro en el dominio.
