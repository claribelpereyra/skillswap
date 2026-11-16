# Caso 53: Promociones acumulables con Decorator

## Objetivo

Aplicar promociones acumulables al precio de una contratación y garantizar que el total nunca sea negativo.

## Cambios realizados

- Se agregó `Price` y `BasePrice` para representar el precio original.
- `PromotionDecorator` permite envolver un precio con otra promoción.
- `FixedPromotion` aplica un descuento fijo y `PercentagePromotion` aplica un porcentaje de 0 a 100.
- Cada descuento se aplica al total decorado anterior; si excede el precio restante, el resultado queda limitado a cero.
- Los cálculos porcentuales evitan desbordamientos incluso para el máximo monto representable.

## Resultado

Las promociones pueden componerse en el orden deseado sin alterar el precio base y sin que el importe final sea negativo.
