# Caso 62: Recomendaciones para el usuario activo

## Objetivo

Exponer recomendaciones para el usuario autenticado, reutilizando los filtros de búsqueda y permitiendo escoger una estrategia concreta.

## Flujo

1. `RecommendationControllerImpl` compara el usuario recibido con `IdentityService.currentUserId`.
2. `RecommendationForm` separa la estrategia solicitada de `SearchForm`, cuyos campos siguen siendo filtros de búsqueda.
3. El controlador elige `SkillAffinityRecommendationStrategy` por defecto o `RecentPopularityRecommendationStrategy` si se solicita.
4. Crea el `SearchFilters` canónico del paquete `application.dto.publication`, incluyendo el identificador opcional del usuario activo.
5. `RecommendPublicationsService` mantiene los filtros al recorrer todas las páginas de candidatos, aplica la estrategia real y pagina los resultados ya ordenados.
6. El contrato devuelve `ControllerResult` y `ErrorType` para errores de autorización y entrada.

## Pruebas

`RecommendationControllerTest` verifica la validación de identidad, la selección y el paso de la instancia de estrategia, la preservación de los filtros de búsqueda y el mapeo de errores.
