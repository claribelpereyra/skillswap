# Caso 53: Promociones acumulables con Decorator

## Objetivo

Aplicar promociones acumulables al precio de una contratación y garantizar que el total nunca sea negativo.

## Cambios realizados

- Se agregó `Price` y `BasePrice` para representar el precio original.
- `PromotionDecorator` permite envolver un precio con otra promoción.
- `FixedPromotion` aplica un descuento fijo y `PercentagePromotion` aplica un porcentaje de 0 a 100.
- Cada descuento se aplica al total decorado anterior; si excede el precio restante, el resultado queda limitado a cero.
- Los cálculos porcentuales evitan desbordamientos incluso para el máximo monto representable.
- `BookingPricingPolicy` calcula el importe por cliente y publicación al crear la contratación; `Booking.price` persiste el precio final y el pago no vuelve a aplicar descuentos.
- Las políticas pueden componer `PromotionDecorator` sin compartir una única cadena global entre todos los clientes y publicaciones.
- Un precio final de cero es válido: la contratación avanza sin insertar movimientos de monto cero.

## Resultado

Las promociones pueden componerse por contratación y quedan reflejadas en el importe persistido; el total nunca es negativo y puede ser cero.
