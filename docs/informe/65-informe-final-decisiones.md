# Caso 65: Informe final de decisiones de diseño

**Área:** wallet, pagos, precios, reintegros y recomendaciones
**Fecha:** 2026-11-15
**Base:** decisiones implementadas y pruebas documentadas en los casos 43–64.

## 1. Propósito y alcance

Este informe reúne las decisiones técnicas del área de SkillSwap a cargo de wallet, precio, pagos, cancelaciones y recomendaciones. Explica las alternativas elegidas, los motivos y sus consecuencias para que puedan revisarse en equipo y defenderse en la entrega.

La aplicación es un backend Java 17 organizado en módulos Maven, sin Spring Boot ni API HTTP. Los casos de uso reciben llamadas desde controllers Java dentro del mismo proceso. La identidad y la pasarela de SkillCoins son simuladas; no se procesa dinero real.

## 2. Decisiones y alternativas

| Tema | Decisión | Alternativa descartada y motivo |
|---|---|---|
| Separación por capas | Mantener el dominio independiente de JPA. Los puertos de persistencia y los casos de uso viven en aplicación; Hibernate, repositorios y mappers viven en infraestructura. | Anotar las entidades de dominio con JPA habría acoplado reglas de negocio al mecanismo de persistencia y debilitado los límites entre módulos. |
| Saldo de wallet | Tratar el ledger de movimientos como fuente de verdad. `Wallet.restore` reconstruye el saldo y valida que coincida con el valor persistido. | Mantener saldo y movimientos como fuentes independientes permitiría que se desincronicen. |
| Pago y reintegro | Preparar y aplicar los movimientos pareados en el dominio, y guardar contratación y wallets dentro de una única unidad de trabajo. | Débitos y créditos independientes permitirían persistencia parcial. No se usa un bloqueo global: las wallets persistidas emplean `@Version` y bloqueo optimista. |
| Tiempo | Inyectar `java.time.Clock` en lugar de crear un puerto propio para consultar la hora. | Un `TimeProvider` duplicaría una abstracción disponible en Java. `Clock.fixed` permite pruebas deterministas. |
| Políticas de reintegro | Usar `RefundStrategy` para intercambiar la política escalonada (100 %, 50 % o 0 %) por una de reintegro completo. Los umbrales son configurables. | Condiciones fijas dentro del caso de uso habrían acoplado la orquestación a una política concreta y dificultado comparar reglas. |
| Promociones | Componer descuentos fijos y porcentuales mediante Decorator, en el orden configurado y sobre el total restante. El resultado se limita a cero. | Codificar combinaciones de descuentos en el servicio de pago haría crecer sus ramas cada vez que aparezca una promoción nueva. |
| Total gratuito | Rechazar un pago cuyo total promocionado sea cero; los movimientos del ledger requieren importes positivos. | Registrar débitos/créditos de cero no aportaría una operación monetaria válida y complicaría la semántica del ledger. |
| Recomendación | Usar `RecommendationStrategy` para intercambiar afinidad de habilidades y popularidad reciente sin cambiar `RecommendPublicationsService`. | Un único orden fijo o condicionales dentro del caso de uso dificultarían sumar y probar otros criterios. |
| Consultas de recomendación | Reunir los candidatos de todas las páginas, consultar metadatos por lote, ordenar y recién entonces aplicar la página solicitada. | Paginar antes de ordenar podría excluir candidatos relevantes de páginas posteriores; consultar metadatos por publicación produciría N+1. |
| Pasarela de créditos | Aislar la autorización detrás del puerto `SkillCoinGateway` y usar un adaptador simulado en desarrollo y tests. La autorización ocurre antes de abrir la unidad de trabajo. | Integrar una pasarela real excede el alcance y añadiría cobros monetarios no requeridos. |
| Comprobante | Consultarlo solo para participantes y después de completar la contratación; el importe proviene del débito efectivamente registrado. | Recalcularlo desde el precio base perdería el descuento aplicado al pagar y podría divergir del ledger. |

## 3. Consecuencias funcionales y de persistencia

### Wallet y operaciones monetarias

La wallet registra cargas, débitos, créditos y reintegros; salvo las cargas, cada movimiento se asocia a una contratación. Los importes deben ser positivos, el saldo no puede ser negativo y los movimientos se exponen en orden inmutable. El pago transfiere el mismo total final del cliente al proveedor y se rechaza si ya existe un movimiento para esa contratación.

Las wallets y sus movimientos se persisten en infraestructura. El mapper restaura el agregado desde el ledger y verifica el saldo persistido; la entidad JPA usa control de versión para detectar escrituras concurrentes. Flyway administra el esquema.

### Pago, cancelación y promociones

El caso de uso carga la contratación y ambas wallets, confirma la transición y ejecuta el pago en la misma unidad de trabajo. Una cancelación pagada calcula el reintegro a partir del importe realmente acreditado al proveedor, libera la franja y persiste el resultado dentro del límite transaccional.

Los descuentos no modifican el precio base guardado en la contratación: se aplican al cobrar. El ledger, el comprobante y el cálculo de reintegro reflejan el importe efectivamente cobrado. La cadena de promociones se configura en el armado de la aplicación; no existe un catálogo persistido de promociones activas.

### Recomendaciones

La afinidad compara habilidades e intereses del perfil activo con las etiquetas de las publicaciones, normalizando las coincidencias. La popularidad cuenta contrataciones recientes elegibles dentro de una ventana configurable. Ambas estrategias conservan los filtros y la identidad del usuario y resuelven empates de forma determinista.

El controller elige afinidad por defecto o popularidad cuando se solicita. Los metadatos se consultan mediante un puerto de salida por lote, manteniendo las reglas de recomendación fuera de SQL y evitando consultas por cada candidato.

## 4. Trazabilidad de los puntos 56, 57, 64 y 65

| Punto | Descripción de lo realizado | Evidencia |
|---|---|---|
| **56** | Se verifica el rechazo del pago por saldo insuficiente sin persistir cambios en saldos, movimientos o estado de la contratación. | Pruebas de aplicación y persistencia descritas en el [informe de pruebas](./64-pruebas-wallet-precio-y-recomendaciones.md). |
| **57** | Se verifica que un segundo pago se rechace sin repetir débitos, créditos, movimientos ni persistencia. | Pruebas de dominio y aplicación descritas en el [informe de pruebas](./64-pruebas-wallet-precio-y-recomendaciones.md). |
| **64** | Se agrupan las pruebas de saldo insuficiente, dos políticas de reintegro, cambio de estrategia de recomendación y promociones acumulables. | [Informe de pruebas de los casos 56–64](./64-pruebas-wallet-precio-y-recomendaciones.md). |
| **65** | Se consolidan las decisiones de diseño, alternativas, consecuencias y evidencia del área de wallet, pagos, precio y recomendaciones. | Este informe final. |

## 5. Evidencia y validación

Las decisiones se respaldan, entre otras, en:

- Dominio: [WalletTest](../../domain/src/test/java/com/uade/skillswap/domain/wallet/WalletTest.java), [BookingPaymentServiceTest](../../domain/src/test/java/com/uade/skillswap/domain/wallet/BookingPaymentServiceTest.java) y [PromotionTest](../../domain/src/test/java/com/uade/skillswap/domain/pricing/PromotionTest.java).
- Aplicación: [BookingPaymentUseCasesTest](../../application/src/test/java/com/uade/skillswap/application/usecases/BookingPaymentUseCasesTest.java), [RecommendPublicationsServiceTest](../../application/src/test/java/com/uade/skillswap/application/usecases/publication/RecommendPublicationsServiceTest.java), [LoadWalletServiceTest](../../application/src/test/java/com/uade/skillswap/application/usecases/wallet/LoadWalletServiceTest.java) y [BookingReceiptServiceTest](../../application/src/test/java/com/uade/skillswap/application/usecases/booking/BookingReceiptServiceTest.java).
- Persistencia: [JpaWalletRepositoryTest](../../infrastructure/src/test/java/com/uade/skillswap/infrastructure/persistence/wallet/JpaWalletRepositoryTest.java) y [JpaRecommendationMetadataQueryTest](../../infrastructure/src/test/java/com/uade/skillswap/infrastructure/persistence/publication/JpaRecommendationMetadataQueryTest.java).
- Controllers: [RecommendationControllerTest](../../controllers/src/test/java/com/uade/skillswap/controllers/publication/RecommendationControllerTest.java) y [WalletControllerTest](../../controllers/src/test/java/com/uade/skillswap/controllers/wallet/WalletControllerTest.java).

La última verificación completa registrada fue `mvn verify -q`: **280 tests ejecutados, 0 fallos, 0 errores y 0 omitidos**. Los tests de persistencia usan H2 en modo MySQL y las migraciones Flyway.

## 6. Límites conocidos

- La pasarela es simulada y no representa dinero real.
- Las promociones se configuran al componer la aplicación; no hay administración ni persistencia de campañas.
- Una promoción que deja el total en cero se rechaza como pago, aunque el precio promocionado nunca sea negativo.
- La recomendación tiene dos estrategias implementadas; el diseño permite sumar otras sin modificar el consumidor.
- El informe consolida el área de wallet, pagos, precio y recomendaciones; no reemplaza los informes por caso ni documenta áreas ajenas a este alcance.

## 7. Informes de referencia

- [Caso 46: Reloj inyectable](./46-puerto-de-reloj.md)
- [Caso 49: Recomendación con estrategia intercambiable](./49-recomendacion-estrategia-intercambiable.md)
- [Caso 50: Wallet y movimientos](./50-wallet-y-movimientos.md)
- [Caso 51: Pago de una contratación](./51-pago-de-contratacion.md)
- [Caso 52: Políticas de reintegro](./52-politicas-de-reintegro.md)
- [Caso 53: Promociones acumulables](./53-promociones-acumulables.md)
- [Caso 55: Casos de uso de wallet](./55-casos-de-uso-wallet.md)
- [Caso 58: Transacciones de pago y cancelación](./58-transacciones-pago-cancelacion.md)
- [Caso 59: Persistencia de wallet y movimientos](./59-persistencia-wallet-movimientos.md)
- [Caso 60: Consultas de recomendación sin N+1](./60-consultas-de-recomendacion-sin-n-plus-1.md)
- [Caso 61: Pasarela simulada de SkillCoins](./61-pasarela-simulada-de-skillcoins.md)
- [Caso 62: Recomendaciones para el usuario activo](./62-controlador-de-recomendaciones.md)
- [Caso 63: Controllers de contratación y wallet](./63-controllers-contratacion-wallet.md)
- [Caso 64: Informe de pruebas de wallet, precio y recomendaciones](./64-pruebas-wallet-precio-y-recomendaciones.md)
