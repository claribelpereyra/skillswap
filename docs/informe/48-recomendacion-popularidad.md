# Caso 48: Recomendación por popularidad reciente

## Objetivo

Mostrar primero las publicaciones con mayor actividad o vigencia reciente. El criterio busca que las ofertas más actuales y visibles aparezcan antes en la lista para el usuario.

## Cambios realizados

- Se extendió `SearchSort` con criterios de popularidad reciente:
  - `RECENT_POPULARITY`
  - `POPULARITY_RECENT`
  - `MOST_POPULAR_RECENT`
- `JpaPublicationSearchQuery` ordena estas propuestas por fecha de publicación más reciente y, en caso de empate, por reputación.
- Se aseguró que el orden siga siendo determinista y paginable con un desempate final por ID.
- El comportamiento quedó preparado para extenderse a métricas de actividad más específicas cuando existan datos de contratación o interacción.

## Resultado

La lista puede favorecer publicaciones que están más vigentes o que fueron reactivadas recientemente, mejorando la sensación de relevancia y actualidad para el usuario.

## Evidencia del código

- `application/src/main/java/com/uade/skillswap/application/ports/in/publication/SearchSort.java`
- `infrastructure/src/main/java/com/uade/skillswap/infrastructure/persistence/publication/JpaPublicationSearchQuery.java`
- `controllers/src/main/java/com/uade/skillswap/controllers/publication/SearchForm.java`
