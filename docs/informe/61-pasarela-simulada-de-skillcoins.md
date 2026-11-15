# Caso 61: Pasarela simulada de SkillCoins

## Objetivo

Permitir cargar créditos mediante un puerto de pasarela simulado, sin integrar cobros reales.

## Flujo

1. `WalletController` valida la identidad activa y expone la operación de carga.
2. `LoadWalletCommand` valida que el usuario exista y que el importe sea positivo.
3. `LoadWalletService` comprueba la wallet y solicita la autorización del gateway fuera de la unidad de trabajo.
4. Si se autoriza, el caso de uso abre una unidad de trabajo, vuelve a cargar la wallet, registra `Wallet.topUp` y persiste el nuevo saldo.
5. Si se rechaza, no se abre una unidad de trabajo ni se guardan cambios.

`SimulatedSkillCoinGateway.authorizeCredit` aprueba importes positivos. Es un reemplazo local para pruebas y desarrollo: no se conecta a un proveedor ni procesa dinero real. No hay un flujo de alta de cuentas en esta aplicación; la operación requiere una wallet ya existente y la creación automática queda a cargo del futuro ciclo de alta.

## Pruebas

`LoadWalletServiceTest` verifica la carga aprobada, el rechazo sin cambios, el error cuando no existe la wallet y que la autorización ocurre fuera de la unidad de trabajo. `WalletControllerTest` verifica que la carga se exponga por el contrato del controlador y que solo la identidad activa pueda solicitarla.
