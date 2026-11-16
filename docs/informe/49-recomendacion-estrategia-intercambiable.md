# Caso 49: Recomendación con estrategia intercambiable

## Objetivo

Permitir que el caso de uso de recomendación cambie de estrategia sin tocar la lógica principal. La idea es encapsular cada criterio de orden en un objeto independiente y poder inyectarlo por configuración o por llamada.

## Cambios realizados

- Se definió la interfaz `RecommendationStrategy` para encapsular el criterio de orden.
- Se agregó `RecommendPublicationsUseCase` para que el caso de uso reciba una estrategia sin depender de una implementación fija.
- Se implementaron estrategias concretas:
  - `SkillAffinityRecommendationStrategy`
  - `RecentPopularityRecommendationStrategy`
- `RecommendPublicationsService` reúne las páginas de candidatos, aplica la estrategia sobre el conjunto completo y pagina el ranking final.
- Se rechaza una estrategia nula en lugar de devolver un resultado sin ordenar.
- Las estrategias acceden a metadatos mediante un puerto compartido y recuperan datos por lote.
- Las pruebas cubren la paginación global y ambas estrategias.

## Resultado

El recomendador queda abierto a extensión y cerrado a modificación: si mañana se agrega una nueva estrategia (por ejemplo, por score de match, actividad, reputación o eventos recientes), basta con crear otra implementación y pasarla al caso de uso.

## Evidencia del código

- `application/src/main/java/com/uade/skillswap/application/ports/in/publication/RecommendationStrategy.java`
- `application/src/main/java/com/uade/skillswap/application/ports/in/publication/RecommendPublicationsUseCase.java`
- `application/src/main/java/com/uade/skillswap/application/usecases/publication/RecommendPublicationsService.java`
- `application/src/main/java/com/uade/skillswap/application/usecases/publication/SkillAffinityRecommendationStrategy.java`
- `application/src/main/java/com/uade/skillswap/application/usecases/publication/RecentPopularityRecommendationStrategy.java`
