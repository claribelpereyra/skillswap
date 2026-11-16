# Caso 47: Recomendación por afinidad de habilidades

## Objetivo

Priorizar publicaciones cuyas etiquetas coinciden con las habilidades e intereses del perfil activo.

## Cambios realizados

- `SkillAffinityRecommendationStrategy` recibe las habilidades e intereses del usuario mediante `RecommendationMetadataQuery`.
- La consulta carga los datos del perfil en una sola operación, normaliza las coincidencias y compara contra las etiquetas de las publicaciones.
- El ranking prioriza la cantidad de coincidencias; los empates se resuelven por reputación, fecha de publicación e ID.
- La estrategia se aplica antes de paginar la respuesta final.

## Resultado

Las recomendaciones reflejan los datos del perfil del usuario activo, sin depender de un texto o etiqueta ingresados en el buscador.

## Evidencia del código

- `application/src/main/java/com/uade/skillswap/application/usecases/publication/SkillAffinityRecommendationStrategy.java`
- `application/src/main/java/com/uade/skillswap/application/ports/out/publication/RecommendationMetadataQuery.java`
- `infrastructure/src/main/java/com/uade/skillswap/infrastructure/persistence/publication/JpaRecommendationMetadataQuery.java`
