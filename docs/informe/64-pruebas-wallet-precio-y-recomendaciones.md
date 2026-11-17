# Informe de pruebas: wallet, precio y recomendaciones (casos 56–64)

**Fecha:** 2026-11-17
**Alcance:** pruebas de saldo insuficiente, pago duplicado, reintegros, estrategia de recomendación y promociones. El caso 64 agrupa cuatro escenarios que también aparecen individualmente en los casos 56, 58, 59 y 60.

## Resumen de ejecución

| Comando | Resultado |
|---|---|
| `mvn verify` | PASS, código de salida 0 |
| Suite completa | 431 tests (domain 139, application 120, infrastructure 95, controllers 57, app 20); 0 fallos, 0 errores, 0 omitidos |

La suite de persistencia usó H2 en modo MySQL y las migraciones Flyway del proyecto. No se inició una aplicación ni se probaron endpoints: SkillSwap no expone una API HTTP y estos casos corresponden a dominio, aplicación y persistencia.

## Relación con el punto 65

Este informe registra la cobertura y el resultado de las pruebas; el [informe final de decisiones](./65-informe-final-decisiones.md) consolida las alternativas de diseño y explica cómo se relacionan los puntos 56, 57, 64 y 65.

## Cobertura por caso

| Caso | Escenario y evidencia | Resultado |
|---|---|---|
| **56** | Saldo insuficiente. `BookingServiceImplTest.saldoInsuficienteNoConfirmaNiGuardaLaContratacion` comprueba saldos intactos y que el caso de uso no confirme ni guarde la contratación. `BookingWalletIntegrationTest.saldoInsuficienteNoModificaWalletsNiContratacion` verifica, con JPA/H2, que la contratación siga aceptada y que los saldos y movimientos persistidos no cambien. | PASS |
| **57** | Doble pago. `BookingServiceImplTest.unSegundoPagoSeRechazaSinTocarWalletsNiGuardarLaContratacion` verifica que pagar una contratación ya confirmada se rechace por transición inválida, sin consultar wallets ni guardar. `BookingWalletIntegrationTest.pagosSimultaneosNoSobregiranLaWalletDesdeSnapshotsObsoletos` cubre el pago concurrente con persistencia real. | PASS |
| **58** | Cancelación con políticas de reintegro distintas. `BookingServiceImplTest.dosEstrategiasCalculanImportesDistintosParaLaMismaCancelacion` compara una política escalonada y una de reintegro completo en el flujo de aplicación; `RefundStrategyTest` cubre las políticas en el dominio. | PASS |
| **59** | Cambio de estrategia de recomendación sin cambiar el caso de uso. `RecommendPublicationsServiceTest.strategiesCanBeSwitchedOnTheSameServiceAndCandidates` invoca la misma instancia del servicio con dos estrategias. | PASS |
| **60** | Promociones acumulables sin precio negativo. `PromotionTest.promotionsAccumulateAndCannotProduceANegativeTotal` comprueba que descuentos encadenados que exceden el precio se limiten a cero; `BookingServiceImplTest.guardaElPrecioPromocionalSegunClienteYPublicacion` verifica que el precio final se persista al solicitar. | PASS |
| **61** | Pasarela simulada de SkillCoins. `LoadWalletServiceTest` cubre autorización aprobada y rechazada, wallet ausente y ejecución de la autorización fuera de la unidad de trabajo. | PASS en la suite completa |
| **62** | Controlador de recomendaciones. `RecommendationControllerTest` cubre estrategia por defecto, selección de popularidad, identidad y validación de formularios. | PASS en la suite completa |
| **63** | Controllers de wallet y contratación. `WalletControllerTest` y `BookingControllerImplTest` cubren identidad activa, delegación de operaciones y validaciones. | PASS en la suite completa |
| **64** | Agrupa estos escenarios: (1) rechazar saldo insuficiente sin cambiar saldos ni estado, cubierto por el caso 56; (2) cancelar y calcular el reintegro con dos políticas, caso 58; (3) cambiar la estrategia sin tocar el caso de uso, caso 59; y (4) acumular promociones sin precio negativo, caso 60. | PASS; los cuatro escenarios están cubiertos por las pruebas citadas en sus filas. |

## Evidencia

- [BookingServiceImplTest.java](../../application/src/test/java/com/uade/skillswap/application/usecases/BookingServiceImplTest.java)
- [RecommendPublicationsServiceTest.java](../../application/src/test/java/com/uade/skillswap/application/usecases/publication/RecommendPublicationsServiceTest.java)
- [BookingWalletIntegrationTest.java](../../infrastructure/src/test/java/com/uade/skillswap/infrastructure/adapters/BookingWalletIntegrationTest.java)
- [JpaWalletRepositoryTest.java](../../infrastructure/src/test/java/com/uade/skillswap/infrastructure/persistence/wallet/JpaWalletRepositoryTest.java)
- [PromotionTest.java](../../domain/src/test/java/com/uade/skillswap/domain/pricing/PromotionTest.java)
- [RefundStrategyTest.java](../../domain/src/test/java/com/uade/skillswap/domain/wallet/RefundStrategyTest.java)
- [LoadWalletServiceTest.java](../../application/src/test/java/com/uade/skillswap/application/usecases/wallet/LoadWalletServiceTest.java)
- [RecommendationControllerTest.java](../../controllers/src/test/java/com/uade/skillswap/controllers/publication/RecommendationControllerTest.java)
- [WalletControllerTest.java](../../controllers/src/test/java/com/uade/skillswap/controllers/wallet/WalletControllerTest.java)
- Reportes de Surefire: `*/target/surefire-reports/TEST-*.xml`
