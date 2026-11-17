# Perfiles, Calificaciones y Reputación: historias de usuario

Cubre el registro y mantenimiento de perfiles (`PROFESSIONAL` y `ORGANIZATION`), la gestión de habilidades del catálogo e intereses, la baja lógica preservando el historial, la vista pública consolidada, la validación de identidad mock en controladores y las calificaciones verificadas con recálculo automático de reputación.

## Actores

- **Usuario autenticado (Mock Identity)**: actor identificado mediante `UserId` provisto por `IdentityService` (sin contraseñas ni OAuth/JWT). Puede actuar como cliente, proveedor o ambos manteniendo un único perfil y una única reputación consolidada.
- **Profesional (`PROFESSIONAL`)**: usuario individual con habilidades, intereses y disponibilidad horaria que ofrece mentorías o servicios, o contrata a otros profesionales.
- **Organización (`ORGANIZATION`)**: entidad que ofrece desafíos colaborativos con cupos múltiples.

## Ciclo de vida del perfil

```
ACTIVO ──(dar de baja)──► INACTIVO
  ▲                           │
  └───(reactivar · D.P. P1)───┘
```

- La transición **dar de baja** surge directamente del enunciado ("La baja de un perfil no elimina operaciones históricas: se realiza baja lógica."). La transición **reactivar** se registra como **decisión pendiente P1** porque el enunciado no especifica si un perfil dado de baja puede volver a activarse; se agregó para que un usuario pueda rehabilitar su perfil conservando su `UserId`, reputación e historial sin duplicar cuenta.
- En estado **INACTIVO**, el perfil rechaza modificaciones (`updateDetails`, `addSkill`, `removeSkill`, `addInterest`, `removeInterest`, `updateAvailability`), `AuthorProfileQuery` lo trata como ausente para impedir nuevas publicaciones, y la búsqueda de publicaciones oculta sus ofertas, preservando intactos los registros históricos.

## Historias

### HU-P01. Creación de perfil

**Como** usuario autenticado en el servicio de identidad mock, **quiero** crear mi perfil indicando nombre visible, biografía, zona horaria y tipo de cuenta (`PROFESSIONAL` o `ORGANIZATION`), **para** operar en la plataforma.

Criterios de aceptación:

1. Dado un `UserId` sin perfil previo, cuando solicito crear el perfil con nombre visible y tipo de cuenta válidos, entonces se crea en estado activo, con reputación `0.0` y `0` calificaciones.
2. Si el nombre visible es nulo/vacío o falta el tipo de cuenta, el dominio rechaza la operación con `InvalidProfileException`.
3. Si ya existe un perfil para ese `UserId`, la aplicación rechaza el alta duplicada.
4. Solo el usuario propietario de la sesión actual (`IdentityService`, validado en el controller) puede crear el perfil asociado a su `UserId`.

### HU-P02. Edición, habilidades, intereses y disponibilidad

**Como** usuario con perfil activo, **quiero** actualizar mis datos personales, habilidades técnicas, intereses y disponibilidad, **para** reflejar mis competencias y alimentar el motor de recomendación por afinidad.

Criterios de aceptación:

1. Las habilidades del perfil se asocian mediante identificadores positivos y sin duplicados del catálogo Composite (`domain.catalog.Skill`, almacenados como `skillIds: Set<Long>`), evitando nombres libres duplicados; los intereses mantienen etiquetas de texto sin espacios superfluos ni duplicados exactos.
2. Un perfil dado de baja lógica (`INACTIVO`) rechaza cualquier modificación lanzando `InactiveProfileException`.
3. Todas las operaciones de escritura validan en el controller que el actor en sesión coincida con el dueño del perfil.

### HU-P03. Baja lógica del perfil

**Como** usuario, **quiero** dar de baja lógica mi perfil, **para** dejar de figurar como oferente activo sin perder mis contrataciones, pagos ni calificaciones históricas.

Criterios de aceptación:

1. La desactivación cambia el estado a inactivo (`active = false`) sin eliminar filas de perfil ni calificaciones en la base de datos.
2. La consulta de tipo de cuenta para publicar (`AuthorProfileQuery`) devuelve vacío cuando el perfil está inactivo.

> **Decisión pendiente P1**: el método `reactivate()` permite volver a `active = true` reutilizando el perfil e historial existentes en lugar de considerar la baja lógica como irreversible (ver sección *Decisiones pendientes*).

### HU-P04. Consulta de perfil público (Patrón Facade)

**Como** cliente o explorador de la plataforma, **quiero** consultar el perfil público de un usuario en una única vista, **para** ver su reputación, habilidades, disponibilidad, publicaciones activas e historial resumido de calificaciones antes de contratar.

Criterios de aceptación:

1. `GetPublicProfileFacade` consolida en `PublicProfileView` los datos del perfil (`ProfileRepository`), las **publicaciones activas** paginadas (`activePublications: Page<PublicationView>` vía `ActivePublicationsQuery`, con listado de publicaciones en estado `PUBLISHED` y su conteo total) y el **historial resumido** de calificaciones recibidas (`recentRatings: Page<RatingView>` vía `RatingRepository`, ordenadas de más reciente a más antigua), sin acoplar el dominio de perfiles con publicaciones ni contrataciones.
2. Tanto la lista de publicaciones activas como el historial de calificaciones se entregan de forma paginada (`Page<PublicationView>` y `Page<RatingView>`).
3. Si el perfil no existe, responde con `ResourceNotFoundException`.

### HU-P05. Registro de calificación verificada y recálculo de reputación (Caso 10)

**Como** participante (cliente o proveedor) de una contratación en estado `COMPLETED`, **quiero** calificar a la contraparte con un puntaje entero de `1` a `5` y un comentario, **para** evaluar su desempeño y actualizar su reputación pública.

Criterios de aceptación:

1. La contratación debe existir y encontrarse en estado `COMPLETED`.
2. Solo un participante verificado (`client` o `provider`) de esa contratación puede calificar, y exclusivamente a su contraparte real (`recipient != author`, comparando `UserId` de forma exacta y sensible a mayúsculas/minúsculas).
3. Cada participante puede emitir **una única calificación** por contratación (garantizado tanto en lógica de aplicación como por restricción única en base de datos, traduciendo colisiones concurrentes a error de negocio).
4. El puntaje debe estar en `[1..5]` (`InvalidRatingException`).
5. Dentro de una única transacción (`UnitOfWork`), se guarda la calificación y se actualiza la reputación del perfil destinatario calculando el promedio simple a partir de la suma acumulada de puntajes y la cantidad de calificaciones, sin redondeo acumulativo ni setter manual de reputación en el dominio.

### HU-P06. Consulta paginada de calificaciones

**Como** usuario, **quiero** consultar el historial paginado de calificaciones recibidas por un perfil, **para** revisar sus evaluaciones previas.

Criterios de aceptación:

1. Devuelve una página (`Page<RatingView>`) ordenada cronológicamente de la más reciente a la más antigua.

## Decisiones tomadas

| # | Pregunta | Decisión |
| --- | --- | --- |
| P1 | ¿Un perfil dado de baja lógica puede volver a reactivarse (`reactivate`)? | Sí. Se mantiene la reactivación (`ProfileController.reactivate`) para que el usuario conserve su `UserId`, reputación e historial sin crear otro perfil. |
