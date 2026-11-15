# Caso 59: Persistencia de wallet y movimientos

## Objetivo

Persistir el saldo y el historial de cada wallet para recuperar sus datos entre transacciones y reinicios.

## Cambios realizados

- La migración `V4__wallets_and_movements.sql` crea `wallets` y `wallet_movements`, con claves, restricciones de saldo y tipo, secuencia por wallet e índice por contratación.
- `WalletJpaEntity` y `WalletMovementJpaEntity` representan las tablas sin acoplar el dominio a JPA.
- `WalletMapper` convierte el agregado y su ledger, valida que el saldo persistido coincida con los movimientos y solo agrega movimientos nuevos al guardar.
- `JpaWalletRepository` implementa búsqueda por propietario y guardado transaccional, con bloqueo optimista para controlar escrituras concurrentes.
- `JpaWalletRepositoryTest` verifica la lectura y escritura del ledger y que un pago pueda reintegrarse correctamente después de recargar las wallets.

## Resultado

El saldo y los movimientos se recuperan como un agregado coherente y el historial persistido se mantiene inmutable.
