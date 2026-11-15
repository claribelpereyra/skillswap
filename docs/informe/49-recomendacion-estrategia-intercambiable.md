# Caso 49: Recomendación con estrategia intercambiable

## Objetivo

Permitir que el caso de uso de recomendación cambie de estrategia sin tocar la lógica principal. La idea es encapsular cada criterio de orden en un objeto independiente y poder inyectarlo por configuración o por llamada.

## Cambios realizados

- Se definió la interfaz `RecommendationStrategy` para encapsular el criterio de orden.
- Se agregó `RecommendPublicationsUseCase` para que el caso de uso reciba una estrategia sin depender de una implementación fija.
- Se implementaron estrategias concretas:
  - `SkillAffinityRecommendationStrategy`
  - `RecentPopularityRecommendationStrategy`
- `RecommendPublicationsService` aplica la estrategia sobre el resultado de la búsqueda sin modificar el flujo principal ni los contratos existentes.
- Se agregó una prueba que valida que la estrategia puede cambiarse sin tocar el caso de uso.

## Resultado

El recomendador queda abierto a extensión y cerrado a modificación: si mañana se agrega una nueva estrategia (por ejemplo, por score de match, actividad, reputación o eventos recientes), basta con crear otra implementación y pasarla al caso de uso.

## Evidencia del código

- `application/src/main/java/com/uade/skillswap/application/ports/in/publication/RecommendationStrategy.java`
- `application/src/main/java/com/uade/skillswap/application/ports/in/publication/RecommendPublicationsUseCase.java`
- `application/src/main/java/com/uade/skillswap/application/usecases/publication/RecommendPublicationsService.java`
- `application/src/main/java/com/uade/skillswap/application/usecases/publication/SkillAffinityRecommendationStrategy.java`
- `application/src/main/java/com/uade/skillswap/application/usecases/publication/RecentPopularityRecommendationStrategy.java`
