# Caso 54: Comprobante de la contratación

## Objetivo

Consultar el comprobante de una contratación una vez completada, con sus datos de pago y finalización.

## Cambios realizados

- `GetBookingReceiptService.getReceipt` busca la contratación mediante `BookingRepository`.
- La consulta solo se habilita a sus participantes y cuando el estado es `COMPLETED`.
- El servicio obtiene el débito asociado al identificador de contratación desde la billetera del cliente, usando `WalletRepository`.
- `BookingReceipt` devuelve el identificador, cliente, proveedor, importe pagado, fecha del pago y fecha de finalización.
- Se rechazan comprobantes sin pago registrado o cuyo importe no coincide con el precio de la contratación.
- `BookingReceiptServiceTest` verifica acceso antes y después de completar la contratación, permisos y errores de recursos inexistentes.

## Resultado

El comprobante queda disponible solo para las partes de una contratación completada y refleja el pago guardado en el historial de la billetera.
