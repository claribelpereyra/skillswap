# Caso 54: Comprobante de la contratación

## Objetivo

Consultar el comprobante de una contratación una vez completada, con sus datos de pago y finalización.

## Cambios realizados

- `GetBookingReceiptService.getReceipt` busca la contratación mediante `BookingRepository`.
- La consulta solo se habilita a sus participantes y cuando el estado es `COMPLETED`.
- El servicio obtiene el débito asociado al identificador de contratación desde la billetera del cliente, usando `WalletRepository`.
- `BookingReceipt` devuelve el identificador, cliente, proveedor, importe pagado, fecha del pago y fecha de finalización.
- Se rechazan comprobantes sin pago registrado o cuyo importe supera el precio base de la contratación; un importe inferior refleja una promoción aplicada al cobrar.
- `BookingReceiptServiceTest` verifica disponibilidad, permisos, datos del comprobante, pago con descuento y errores de recursos inexistentes.

## Resultado

El comprobante queda disponible solo para las partes de una contratación completada y toma como importe el débito real guardado en la wallet, sin exceder el precio base.
