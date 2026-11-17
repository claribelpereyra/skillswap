# Caso 59: Persistencia de wallet y movimientos

## Objetivo

Persistir el saldo y el historial de cada wallet para recuperar sus datos entre transacciones y reinicios.

## Cambios realizados

- La migración `V6__wallets.sql` crea `wallet` y `wallet_movement`, con claves, restricciones de saldo y tipo, referencia a contratación e índice por contratación.
- `WalletJpaEntity` y `WalletMovementJpaEntity` representan las tablas sin acoplar el dominio a JPA.
- `WalletMapper` reconstruye el agregado y su ledger mediante `Wallet.restore`, que calcula el saldo desde el ledger; al guardar, solo agrega movimientos nuevos.
- `JpaWalletRepository` implementa búsqueda por propietario y guardado transaccional; `WalletJpaEntity` declara `@Version` para control optimista.
- `JpaWalletRepositoryTest` verifica lectura y escritura del ledger y el conflicto optimista; `BookingWalletIntegrationTest` cubre pago, saldo insuficiente y reintegro con persistencia real.

## Resultado

El saldo y los movimientos se recuperan como un agregado coherente y el historial persistido se mantiene inmutable.
