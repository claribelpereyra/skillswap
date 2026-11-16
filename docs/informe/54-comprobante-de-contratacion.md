# Caso 54: Comprobante de la contratación

## Objetivo

Consultar el comprobante de una contratación una vez completada, con sus datos de pago y finalización.

## Cambios realizados

- `GetBookingReceiptService.getReceipt` busca la contratación mediante `BookingRepository`.
- La consulta solo se habilita a sus participantes y cuando el estado es `COMPLETED`.
- El servicio obtiene el débito asociado al identificador de contratación desde la billetera del cliente, usando `WalletRepository`.
- `application.dto.booking.BookingReceipt` devuelve IDs e importe como valores simples, junto con las fechas de pago y finalización.
- Se rechazan comprobantes sin pago registrado o cuyo importe no coincide con el precio de la contratación.
- `BookingReceiptServiceTest` verifica disponibilidad, permisos, datos del comprobante y errores de recursos inexistentes.

## Resultado

El comprobante queda disponible solo para las partes de una contratación completada y refleja el pago guardado en el historial de la billetera.
