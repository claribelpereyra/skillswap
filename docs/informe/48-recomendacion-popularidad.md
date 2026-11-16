# Caso 48: Recomendación por popularidad reciente

## Objetivo

Priorizar publicaciones con más contrataciones aceptadas, confirmadas, en curso o completadas dentro de una ventana temporal.

## Cambios realizados

- `RecentPopularityRecommendationStrategy` solicita los conteos de las publicaciones candidatas en una sola consulta por lote.
- La consulta usa la fecha de creación de la contratación y excluye contrataciones pendientes, rechazadas y canceladas.
- La ventana y el reloj son explícitos y se pueden fijar en pruebas.
- Los empates se resuelven por fecha de publicación, reputación e ID.

## Resultado

La popularidad representa actividad de contratación reciente y no se sustituye por la fecha de publicación.

## Evidencia del código

- `application/src/main/java/com/uade/skillswap/application/usecases/publication/RecentPopularityRecommendationStrategy.java`
- `application/src/main/java/com/uade/skillswap/application/ports/out/publication/RecommendationMetadataQuery.java`
- `infrastructure/src/main/java/com/uade/skillswap/infrastructure/persistence/publication/JpaRecommendationMetadataQuery.java`
