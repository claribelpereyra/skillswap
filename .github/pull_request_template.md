## Qué cambia

<!-- Qué hace este PR y por qué. Si cierra una historia o un caso de aceptación del enunciado, mencionalo. -->

## Decisiones de diseño

<!-- Si agrega un patrón, una entidad o mueve algo de capa: alternativa elegida y por qué, en dos o tres líneas. Si no aplica, borrar la sección. -->

## Cómo probarlo

<!-- Tests nuevos o pasos para verificarlo a mano. -->

## Checklist

- [ ] La rama sale de `develop` y el PR apunta a `develop` (salvo cierre de hito)
- [ ] `mvn verify` pasa localmente
- [ ] Los casos negativos que toca el cambio tienen su test
- [ ] No agrega dependencias entre módulos ni librerías nuevas sin haberlo hablado
- [ ] No edita migraciones de Flyway ya mergeadas
- [ ] Si toca `docs/uml/*.mdj`, avisé en el grupo
