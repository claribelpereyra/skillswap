# Caso 61: Pasarela simulada de SkillCoins

## Objetivo

Permitir cargar créditos mediante un puerto de pasarela simulado, sin integrar cobros reales.

## Flujo

1. `WalletController` valida la identidad activa y expone la operación de carga.
2. `LoadWalletCommand` valida que la identidad y el importe positivo estén presentes.
3. `LoadWalletService` solicita la autorización del gateway fuera de la unidad de trabajo.
4. Si se autoriza, el caso de uso abre una unidad de trabajo, carga la wallet o crea una con `Wallet.open`, registra `Wallet.topUp` y persiste el saldo.
5. Si se rechaza, no se abre una unidad de trabajo ni se guardan cambios.

`SimulatedSkillCoinGateway.authorizeCredit` aprueba importes positivos. Es un reemplazo local para pruebas y desarrollo: no se conecta a un proveedor ni procesa dinero real. La primera carga crea la wallet del usuario si todavía no existe.

## Pruebas

`LoadWalletServiceTest` verifica la carga aprobada, la creación de una wallet ausente, el rechazo sin cambios y que la autorización ocurre fuera de la unidad de trabajo. `WalletControllerTest` verifica que la carga se exponga por el contrato del controlador y que solo la identidad activa pueda solicitarla.
