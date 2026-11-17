# Decisiones de diseño: Perfiles, Calificaciones, Reputación e Identidad Mock

**Área responsable:** Andy (`domain.profile`, `domain.rating`, `application`, `infrastructure`, `controllers`)

---

## 1. Alcance del área y actores

El área modela la identidad de dominio de los participantes de **SkillSwap** (`UserId` validado contra `IdentityService`), los perfiles duales (`PROFESSIONAL` / `ORGANIZATION`), sus habilidades vinculadas al catálogo (`domain.catalog.Skill`) e intereses, la baja lógica y el flujo de calificaciones verificadas que alimenta la reputación consultada por la búsqueda y recomendación.

- **Usuario Dual**: un único `UserId` posee un único `Profile` y una única reputación consolidada sin importar si actúa como cliente o proveedor.
- **Identidad y acceso**: sin registro con contraseñas, hashing ni JWT/OAuth (`MockIdentityAdapter` simula la sesión activa); los controllers validan que el `userId` recibido coincida con el usuario autenticado antes de delegar en los casos de uso.

---

## 2. Patrones GoF y GRASP aplicados

| Patrón | Ubicación | Problema que resuelve | Alternativa descartada y motivo |
| --- | --- | --- | --- |
| **Adapter** (GoF) | `MockIdentityAdapter` implementa `IdentityService` | Desacoplar la validación de sesión simulada en controllers del mecanismo concreto de autenticación (DIP). | Pasar flags o variables globales en UI; rompería testabilidad y acoplaría capas. |
| **Facade** (GoF) | `GetPublicProfileFacade` (`GetPublicProfileUseCase`) | Consolidar perfil, reputación, publicaciones activas (`ActivePublicationsQuery`) e historial resumido de calificaciones (`RatingRepository`) en una única consulta. | Hacer que el controller consulte múltiples agregados o acoplar `Profile` con `Publication` / `Booking`. |
| **Repository** (DDD / PoEAA) | `ProfileRepository`, `RatingRepository` + implementaciones JPA | Aislar reglas puras de dominio en `domain` (cero anotaciones JPA) respecto del mapeo relacional en `infrastructure`. | Anotar `@Entity` sobre el agregado de dominio, prohibido por Clean Architecture. |
| **Controller** (GRASP) | `ProfileControllerImpl`, `RatingControllerImpl` | Validar sesión mock (`IdentityService`) y parámetros de entrada en una frontera delgada, pasando el `UserId` autenticado en cada command y devolviendo `ControllerResult<T>`. | Validar sesión dentro de cada caso de uso o devolver un tipo de resultado distinto por módulo. |

---

## 3. Reglas de dominio, sesión y excepciones uniformes

- La identidad se valida **exclusivamente en los controllers** contra `IdentityService`: si el actor falta o no coincide con `identityService.currentUserId()`, se lanza `AccessDeniedException`, traducida por `ControllerResult<T>` a `ErrorType.ACCESS_DENIED`. Los casos de uso reciben el actor ya validado (por ejemplo `RegisterRatingCommand(authorId, ...)`).
- Todas las excepciones de invariantes del dominio (`InvalidProfileException`, `InactiveProfileException`, `InvalidRatingException`) extienden `BusinessRuleException`, por lo que `ControllerResult<T>` las devuelve como `ErrorType.BUSINESS_RULE` sin filtrar detalles internos.
- Las validaciones de formato/entrada en los controllers lanzan `IllegalArgumentException` (`ErrorType.INVALID_INPUT`), las faltas de recurso lanzan `ResourceNotFoundException` (`ErrorType.NOT_FOUND`) y los choques de control de concurrencia optimista lanzan `ConcurrentUpdateException` (`ErrorType.CONFLICT`).
- En `Profile`, las habilidades se almacenan como identificadores del catálogo (`Set<Long> skillIds`) reutilizando el Composite `domain.catalog.Skill` sin duplicar conceptos de habilidad; la reputación se calcula al leerla como promedio simple desde `ratingSum` y `ratingCount` (`Math.round(ratingSum * 100.0 / ratingCount) / 100.0`) para evitar error acumulativo de redondeo, sin setter manual fuera de `registerRating(int score)` (`rebuild(...)` queda reservado para reconstrucción desde persistencia). En `Rating`, los `UserId` se comparan con `equals` (sensible a mayúsculas/minúsculas, consistente con `Booking`).
- En `RegisterRatingService` (**Caso de aceptación 10**), la validación de contratación `COMPLETED` vía `EligibleBookingQuery`, pertenencia y contraparte real, verificación previa de unicidad, guardado en `RatingRepository` y recálculo en `Profile` se ejecutan **dentro de un mismo `UnitOfWork`**, evitando inconsistencias ante fallos o concurrencia.

---

## 4. Diseño de persistencia relacional, concurrencia y prevención de $N+1$

La migración **`V5__profiles_and_ratings.sql`** (numerada después de `V4__booking_foreign_keys.sql`, a coordinar solo si otro módulo pendiente como billetera `#33` usa `V4`) sigue la convención de tablas en singular e inglés (`booking`, `publication`):

1. **`profile`**: `id`, `user_id` (`UNIQUE`), `display_name`, `bio`, `time_zone`, `account_type`, `reputation`, `rating_sum`, `rating_count`, `availability`, `active`, `created_at`, `version`.
   - Incorpora `@Version` (`version`) y bloqueo optimista (`LockModeType.OPTIMISTIC_FORCE_INCREMENT` en `JpaProfileRepository.save`) para evitar pérdida de actualizaciones cuando dos calificaciones simultáneas actualizan el mismo perfil (`ConcurrentUpdateException` -> `ErrorType.CONFLICT`).
   - Sus nombres coinciden con las constantes de `JpaPublicationSearchQuery` (`PROFILES_TABLE = "profile"`, `PROFILE_USER_ID = "user_id"`, `PROFILE_REPUTATION = "reputation"`, `PROFILE_ACTIVE = "active"`), centralizando el único punto SQL compartido con la búsqueda de publicaciones.
2. **`profile_skill`** (`PRIMARY KEY (profile_id, skill_id)`, FK `fk_profile_skill_profile` -> `profile(id) ON DELETE CASCADE`) y **`profile_interest`** (`PRIMARY KEY (profile_id, interest)`, FK `fk_profile_interest_profile` -> `profile(id) ON DELETE CASCADE`).
3. **`rating`**: `id`, `booking_id`, `author_id`, `recipient_id`, `score`, `comment`, `rated_at`.
   - Restricciones físicas: `fk_rating_booking` (`FOREIGN KEY (booking_id) REFERENCES booking (id)`), `uq_rating_booking_author` (`UNIQUE (booking_id, author_id)`), `ck_rating_score` (`CHECK (score BETWEEN 1 AND 5)`) y `ck_rating_not_self` (`CHECK (author_id <> recipient_id)`).
   - Ante envíos simultáneos duplicados sobre la misma contratación, `JpaRatingRepository.save` ejecuta `persist` + `flush` y traduce la violación de `uq_rating_booking_author` a `InvalidRatingException` (`ErrorType.BUSINESS_RULE`).
   - Índice **`idx_rating_recipient_rated_at`** sobre `(recipient_id, rated_at)` para paginación eficiente del historial recibido.

La demo local única se carga con `java -jar app/target/app-0.1.0-SNAPSHOT.jar --demo`: `DemoScenario` crea los perfiles Mateo/Sofía, habilidades, ofertas y saldo inicial mediante los controllers reales. Es idempotente y opcional; sin `--demo` la aplicación solo aplica las migraciones de esquema. Se retiró el antiguo SQL de ejemplo no ejecutado para evitar dos conjuntos de datos distintos. Los tests de perfiles/calificaciones conservan sus propios fixtures aislados.

### Prevención de consultas $N+1$ y desacoplamiento entre módulos

- `JpaProfileRepository.findByUserId` carga `skillIds` e `interests` con `LEFT JOIN FETCH` en una sola consulta sobre `ProfileJpaEntity`, evitando lecturas diferidas y `LazyInitializationException`.
- `JpaRatingRepository.findByRecipientId` ejecuta consulta paginada (`COUNT` + `SELECT` con `ORDER BY r.ratedAt DESC, r.id DESC`), evitando traer el historial completo a memoria.
- `JpaAuthorProfileQuery` proyecta únicamente `accountType` de perfiles activos sin hidratar colecciones.
- `JpaEligibleBookingQuery` y `JpaActivePublicationsQuery` delegan en los repositorios públicos compartidos `BookingRepository.findById` y `PublicationRepository.findByAuthor` (`PublicationStatus.PUBLISHED`), eliminando SQL nativo directo sobre tablas de otros módulos.
