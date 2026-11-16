# Caso 59: Persistencia de wallet y movimientos

## Objetivo

Persistir el saldo y el historial de cada wallet para recuperar sus datos entre transacciones y reinicios.

## Cambios realizados

- La migración `V5__wallets_and_movements.sql` crea `wallet` y `wallet_movement`, con claves, restricciones de saldo y tipo, secuencia por wallet, referencia a contratación e índice por contratación.
- `WalletJpaEntity` y `WalletMovementJpaEntity` representan las tablas sin acoplar el dominio a JPA.
- `WalletMapper` reconstruye el agregado y su ledger mediante `Wallet.restore`, que compara el saldo reconstruido con el valor persistido; al guardar, solo agrega movimientos nuevos.
- `JpaWalletRepository` implementa búsqueda por propietario y guardado transaccional; `WalletJpaEntity` declara `@Version` para control optimista.
- `JpaWalletRepositoryTest` verifica lectura y escritura del ledger, rollback y que un pago pueda reintegrarse correctamente después de recargar las wallets.

## Resultado

El saldo y los movimientos se recuperan como un agregado coherente y el historial persistido se mantiene inmutable.
