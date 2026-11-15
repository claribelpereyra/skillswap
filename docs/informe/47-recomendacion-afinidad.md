# Caso 47: Recomendación por afinidad de habilidades

## Objetivo

Priorizar publicaciones que más se alinean con el perfil del usuario o con una habilidad de interés. La idea es que la búsqueda muestre primero aquellas ofertas más cercanas a la experiencia, intereses o competencias del cliente.

## Cambios realizados

- Se extendió `SearchSort` con criterios de afinidad de habilidades:
  - `SKILL_MATCH`
  - `SKILL_AFFINITY`
  - `AFFINITY`
- La consulta de publicaciones en JPA incorpora un orden que prioriza coincidencias con la habilidad elegida.
- Cuando la búsqueda tiene una habilidad vinculada, la recomendación muestra primero las publicaciones con match directo sobre esa habilidad.
- Los desempates se resuelven por reputación del autor y luego por la fecha más reciente.
- El controller y el form de búsqueda fueron ajustados para aceptar esta clasificación desde la UI.

## Resultado

El sistema puede priorizar publicaciones con mayor afinidad de habilidades, reduciendo la fricción de la búsqueda y mostrando primero ofertas más relevantes para cada usuario.

## Evidencia del código

- `application/src/main/java/com/uade/skillswap/application/ports/in/publication/SearchSort.java`
- `infrastructure/src/main/java/com/uade/skillswap/infrastructure/persistence/publication/JpaPublicationSearchQuery.java`
- `controllers/src/main/java/com/uade/skillswap/controllers/publication/SearchForm.java`
