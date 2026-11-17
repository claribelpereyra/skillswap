# Caso 45: Patrones candidatos, variaciones y pruebas

| Patrón | Variación que resuelve | Cómo se verifica |
|---|---|---|
| **State** | El ciclo de una contratación cambia según su estado; cada estado permite o rechaza acciones como aceptar, pagar, cancelar, iniciar y completar. | En `BookingTest`, cubrir transiciones válidas e inválidas, actor no autorizado y doble confirmación de pago. `StateHistory` devuelve una copia inmutable y registra actor y fecha; ver brecha de motivo abajo. |
| **Strategy** | Permite variar el criterio de recomendación (afinidad o popularidad reciente) y la política de reintegro sin modificar los casos de uso consumidores. | En `RecommendPublicationsServiceTest`, verificar ambos rankings y la paginación global; en `RefundStrategyTest` y `BookingServiceImplTest`, comparar ambas políticas de reintegro y cubrir umbrales y redondeo hacia abajo. |
| **Decorator** | Los descuentos fijos y porcentuales se componen sin modificar el precio base ni acoplar cada promoción a las demás. | En `PromotionTest`, verificar orden de aplicación, acumulación, límite inferior cero y ausencia de desbordamiento; en `BookingServiceImplTest`, comprobar que se persiste el precio promocional final. |
| **Adapter** | El puerto `SkillCoinGateway` permite sustituir la pasarela simulada por otra implementación sin cambiar el caso de uso. | En `LoadWalletServiceTest`, comprobar autorización aprobada y rechazada, que ocurre fuera de la unidad de trabajo y que el rechazo no persiste cambios. |
| **Repository + Unit of Work** | Separan el acceso a wallets/contrataciones del dominio y agrupan cambios de pago o cancelación para evitar persistencia parcial. | `BookingServiceImplTest` verifica que un saldo insuficiente no guarda la contratación; `BookingWalletIntegrationTest` valida rollback persistente y `JpaWalletRepositoryTest` la lectura del ledger. |
| **Bloqueo optimista (`@Version`)** | Resuelve pagos concurrentes sobre wallets cargadas por transacciones distintas, sin bloquear globalmente las operaciones. | En `JpaWalletRepositoryTest`, iniciar dos unidades de trabajo sobre la misma wallet: una operación debe confirmarse y la otra fallar con `ConcurrentUpdateException`, dejando un solo débito persistido. |

## Criterio de selección

Se mantiene cada patrón donde encapsula una variación real: políticas de reintegro, composición de descuentos, estados de contratación, adaptadores externos y persistencia concurrente. Se evita agregar abstracciones cuando no hay una segunda implementación o una variación que probar.

## Reglas de negocio verificadas

- **Motivo en cada transición:** cada estado registra un motivo por defecto cuando la persona no escribe uno (por ejemplo "Accepted by the provider"); rechazo y cancelación usan el motivo ingresado (#52).
- **Calificaciones únicas:** `RegisterRatingService` solo admite participantes de una contratación completada, y la restricción `uq_rating_booking_author` impide calificar dos veces.
- **Baja lógica de perfil:** `DeactivateProfileService` marca el perfil como inactivo sin borrar contrataciones, calificaciones ni movimientos.
- **Permisos en controllers:** se obtiene el usuario activo del login mock y se lo pasa a los casos de uso; la autorización por participante/propietario debe permanecer validada en aplicación o dominio, además de la validación del identificador activo.
