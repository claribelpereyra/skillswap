# Caso 44: Historias de wallet, pago, cancelación y promociones

## HU-44.1 Consultar saldo y movimientos

**Como** usuario, **quiero** consultar el saldo y el historial de mi wallet, **para** conocer mis créditos y las operaciones que los modificaron.

### Criterios de aceptación

- Solo se consulta la wallet del usuario autenticado.
- La respuesta contiene el saldo y los movimientos ordenados, sin exponer entidades de persistencia.
- Cada movimiento tiene un importe positivo; un débito no puede dejar saldo negativo.
- El historial conserva la contratación asociada a cada pago, crédito o reintegro.

## HU-44.2 Cargar SkillCoins

**Como** usuario, **quiero** cargar SkillCoins mediante una pasarela simulada, **para** poder pagar contrataciones.

### Criterios de aceptación

- El importe debe ser positivo y la identidad activa debe coincidir con el dueño de la wallet.
- La autorización del gateway ocurre antes de abrir la unidad de trabajo.
- Si la autorización se aprueba y la wallet no existe, se crea durante la operación y se registra la carga.
- Si se rechaza, no se crea ni modifica una wallet.

## HU-44.3 Pagar una contratación

**Como** cliente, **quiero** pagar una contratación aceptada con mi wallet, **para** confirmar el acuerdo y acreditar al proveedor.

### Criterios de aceptación

- El pago usa el precio guardado en la contratación y aplica las promociones configuradas al momento del cobro.
- Se debita al cliente y se acredita al proveedor el mismo importe final.
- El pago y la confirmación de la contratación se guardan en una unidad de trabajo.
- Un saldo insuficiente, un pago duplicado o una validación fallida no deja movimientos ni cambios parciales.

## HU-44.4 Cancelar una contratación y calcular el reintegro

**Como** participante, **quiero** cancelar una contratación según la política de reintegro, **para** liberar la franja y recuperar el importe que corresponda.

### Criterios de aceptación

- Una cancelación aceptada libera la franja; una cancelación pagada también reintegra según la anticipación.
- Las políticas pueden devolver el 100 %, 50 % o 0 %; el 50 % de un monto impar redondea hacia abajo.
- El cálculo parte del importe efectivamente cobrado, después de promociones.
- Un reintegro duplicado se rechaza; un reintegro de cero no agrega movimientos de importe cero.
- La cancelación, la liberación de la franja y los movimientos se guardan juntos.

## HU-44.5 Aplicar promociones al cobrar

**Como** cliente, **quiero** que los descuentos configurados se apliquen al pagar, **para** conocer y abonar el precio final de la contratación.

### Criterios de aceptación

- Los descuentos fijos y porcentuales se acumulan en el orden configurado, cada uno sobre el total restante.
- El descuento no puede producir un precio negativo.
- El ledger registra el mismo importe final que se debita y acredita; el comprobante informa ese importe y el reintegro lo toma como base.
- Un total final de cero no se cobra: el ledger actual requiere movimientos positivos.

La cadena de promociones se inyecta en el servicio de pago. No hay un catálogo persistido de promociones activas; la selección y configuración de la cadena quedan a cargo del armado de la aplicación.

## Evidencia

- `LoadWalletServiceTest`: autorización, creación de wallet ausente y rechazo sin cambios.
- `BookingServiceImplTest`: pago, saldo insuficiente, doble pago, cancelación, reintegro y precio promocional en el flujo de aplicación.
- `WalletTest` y `PromotionTest`: movimientos pareados, reintegro duplicado, descuentos acumulados y saldo insuficiente.
- `BookingReceiptServiceTest`: comprobante con el importe realmente cobrado.
- `JpaWalletRepositoryTest`: persistencia, rollback y concurrencia.
