# Caso 47: Recomendación por afinidad de habilidades

## Objetivo

Priorizar publicaciones que más se alinean con el perfil del usuario o con una habilidad de interés. La idea es que la búsqueda muestre primero aquellas ofertas más cercanas a la experiencia, intereses o competencias del cliente.

## Cambios realizados

- Se extendió `SearchSort` con criterios de afinidad de habilidades:
  - `SKILL_MATCH`
  - `SKILL_AFFINITY`
  - `AFFINITY`
- La consulta en JPA cruza las habilidades e intereses del perfil activo con las etiquetas de cada publicación; también considera una habilidad seleccionada en la búsqueda.
- Las coincidencias se comparan sin distinguir mayúsculas/minúsculas y se ordenan por cantidad de etiquetas coincidentes.
- Los desempates se resuelven por reputación, fecha de publicación e ID, en ese orden y de mayor a menor.
- El controller y el form de búsqueda fueron ajustados para aceptar esta clasificación desde la UI.

## Resultado

El sistema puede priorizar publicaciones con mayor afinidad de habilidades, reduciendo la fricción de la búsqueda y mostrando primero ofertas más relevantes para cada usuario.

## Evidencia del código

- `application/src/main/java/com/uade/skillswap/application/ports/in/publication/SearchSort.java`
- `infrastructure/src/main/java/com/uade/skillswap/infrastructure/persistence/publication/JpaPublicationSearchQuery.java`
- `controllers/src/main/java/com/uade/skillswap/controllers/publication/SearchForm.java`
