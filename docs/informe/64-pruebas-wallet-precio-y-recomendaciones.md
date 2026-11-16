# Informe de pruebas: wallet, precio y recomendaciones (casos 56–64)

**Fecha:** 2026-11-15
**Alcance:** pruebas de saldo insuficiente, pago duplicado, reintegros, estrategia de recomendación y promociones. El caso 64 agrupa cuatro escenarios que también aparecen individualmente en los casos 56, 58, 59 y 60.

## Resumen de ejecución

| Comando | Resultado |
|---|---|
| `mvn -pl domain,application test -q` | PASS, código de salida 0 |
| `mvn verify -q` | PASS, código de salida 0 |
| Suite completa | 280 tests; 0 fallos, 0 errores, 0 omitidos |

La suite de persistencia usó H2 en modo MySQL y las migraciones Flyway del proyecto. No se inició una aplicación ni se probaron endpoints: SkillSwap no expone una API HTTP y estos casos corresponden a dominio, aplicación y persistencia.

## Relación con el punto 65

Este informe registra la cobertura y el resultado de las pruebas; el [informe final de decisiones](./65-informe-final-decisiones.md) consolida las alternativas de diseño y explica cómo se relacionan los puntos 56, 57, 64 y 65.

## Cobertura por caso

| Caso | Escenario y evidencia | Resultado |
|---|---|---|
| **56** | Saldo insuficiente. `BookingPaymentUseCasesTest.saldoInsuficienteNoPersisteSaldoNiEstadoDeContratacion` comprueba saldos intactos y que el caso de uso no guarde la contratación ni las wallets. `JpaWalletRepositoryTest.saldoInsuficienteNoPersisteCambiosDeWalletNiEstadoDeContratacion` verifica, con JPA/H2 y una nueva lectura, que la contratación siga aceptada y que los saldos y movimientos persistidos no cambien. | PASS |
| **57** | Doble pago. `BookingPaymentUseCasesTest.unPagoDuplicadoNoVuelveAfectarSaldosMovimientosNiEstado` verifica rechazo, estado confirmado sin transición adicional, saldos y movimientos sin cambios y una sola persistencia. El dominio también cubre el pago duplicado en `BookingPaymentServiceTest.paymentDebitsClientAndCreditsProviderOnce`. | PASS |
| **58** | Cancelación con políticas de reintegro distintas. `BookingPaymentUseCasesTest.dosPoliticasDeReintegroProducenResultadosDistintosAlCancelar` verifica una política escalonada (50 %) y una de reintegro completo (100 %) en el flujo de aplicación. | PASS |
| **59** | Cambio de estrategia de recomendación sin cambiar el caso de uso. `RecommendPublicationsServiceTest.elCasoDeUsoAceptaOtraEstrategiaSinModificarSuImplementacion` invoca la misma instancia del servicio con dos estrategias y verifica rankings opuestos. | PASS |
| **60** | Promociones acumulables sin precio negativo. `PromotionTest.promocionesAcumuladasNoDejanPrecioNegativo` comprueba que descuentos encadenados que exceden el precio se limiten a cero. `BookingPaymentServiceTest` verifica además el importe neto cobrado y el rechazo de un pago gratuito, sin movimientos parciales. | PASS |
| **61** | Pasarela simulada de SkillCoins. `LoadWalletServiceTest` cubre autorización aprobada y rechazada, wallet ausente y ejecución de la autorización fuera de la unidad de trabajo. | PASS en la suite completa |
| **62** | Controlador de recomendaciones. `RecommendationControllerTest` cubre estrategia por defecto, selección de popularidad, identidad y validación de formularios. | PASS en la suite completa |
| **63** | Controllers de wallet y contratación. `WalletControllerTest` y `BookingControllerImplTest` cubren identidad activa, delegación de operaciones y validaciones. | PASS en la suite completa |
| **64** | Agrupa estos escenarios: (1) rechazar saldo insuficiente sin cambiar saldos ni estado, cubierto por el caso 56; (2) cancelar y calcular el reintegro con dos políticas, caso 58; (3) cambiar la estrategia sin tocar el caso de uso, caso 59; y (4) acumular promociones sin precio negativo, caso 60. | PASS; los cuatro escenarios están cubiertos por las pruebas citadas en sus filas. |

## Cambios realizados

- Se añadieron pruebas de aplicación para rechazar el doble pago sin cambios posteriores y para comparar dos políticas de reintegro al cancelar.
- Se añadió una prueba de recomendación que cambia la estrategia sin reemplazar el caso de uso.
- Se añadió una prueba de persistencia JPA para asegurar que un pago con saldo insuficiente no altere el estado guardado de la contratación, saldos o ledger.
- Se renombraron en español los tests de saldo insuficiente y de promociones acumulables.
- No se modificó código de producción.

## Evidencia

- [BookingPaymentUseCasesTest.java](../../application/src/test/java/com/uade/skillswap/application/usecases/BookingPaymentUseCasesTest.java)
- [RecommendPublicationsServiceTest.java](../../application/src/test/java/com/uade/skillswap/application/usecases/publication/RecommendPublicationsServiceTest.java)
- [JpaWalletRepositoryTest.java](../../infrastructure/src/test/java/com/uade/skillswap/infrastructure/persistence/wallet/JpaWalletRepositoryTest.java)
- [PromotionTest.java](../../domain/src/test/java/com/uade/skillswap/domain/pricing/PromotionTest.java)
- [BookingPaymentServiceTest.java](../../domain/src/test/java/com/uade/skillswap/domain/wallet/BookingPaymentServiceTest.java)
- [LoadWalletServiceTest.java](../../application/src/test/java/com/uade/skillswap/application/usecases/wallet/LoadWalletServiceTest.java)
- [RecommendationControllerTest.java](../../controllers/src/test/java/com/uade/skillswap/controllers/publication/RecommendationControllerTest.java)
- [WalletControllerTest.java](../../controllers/src/test/java/com/uade/skillswap/controllers/wallet/WalletControllerTest.java)
- Reportes de Surefire: `*/target/surefire-reports/TEST-*.xml`
