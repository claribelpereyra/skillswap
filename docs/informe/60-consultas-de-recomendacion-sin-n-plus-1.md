# Caso 60: Consultas de recomendación sin N+1

## Objetivo

Reutilizar la búsqueda filtrada de publicaciones y ordenar sus resultados con estrategias intercambiables, sin delegar la selección de estrategia a un enum de ordenamiento SQL.

## Flujo de afinidad

1. `RecommendPublicationsService` solicita todas las páginas de candidatos con los mismos filtros y tamaño máximo de página.
2. `SkillAffinityRecommendationStrategy` consulta en un lote las habilidades e intereses del perfil activo mediante `RecommendationMetadataQuery`.
3. La estrategia ordena los candidatos por coincidencias de etiquetas, reputación y un desempate determinístico.
4. El caso de uso aplica la página solicitada después del ordenamiento, de modo que una coincidencia de una página posterior no quede fuera de las recomendaciones.

`JpaRecommendationMetadataQuery` lee solo perfiles activos. Usa las constantes `PROFILE_USER_ID` y `PROFILE_ACTIVE` declaradas junto con el esquema de perfiles en `JpaPublicationSearchQuery`.

## Popularidad reciente

`RecentPopularityRecommendationStrategy` ordena por la cantidad de contrataciones iniciadas dentro de su ventana temporal configurable. `JpaRecommendationMetadataQuery.recentBookingCounts` cuenta en una consulta por lote las contrataciones no canceladas cuyo primer evento ocurrió desde el límite temporal; la fecha de publicación no se usa como sustituto de popularidad.

## Consultas y pruebas

La búsqueda mantiene sus filtros de texto, categoría, modalidad, precio, etiqueta y reputación en cada página de candidatos. Las estrategias se aplican antes de paginar la respuesta final. Las pruebas verifican que se ordenen candidatos de varias páginas antes de recortar el resultado, y que los conteos de contrataciones recientes determinen la popularidad aun cuando la publicación menos popular sea más nueva.
