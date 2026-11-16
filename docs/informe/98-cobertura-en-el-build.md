# Tarea 98 — Verificación de cobertura

`mvn verify` ejecuta `jacoco:check` en domain y application. Se exige al menos 80 % de instrucciones cubiertas en cada módulo por separado (`BUNDLE / INSTRUCTION / COVEREDRATIO`). No se excluyen clases para alcanzar el mínimo. No se aplica el umbral a infraestructura, controllers ni app; siguen generando sus reportes.

La propiedad compartida `skillswap.coverage.minimum` vale `0.80`. El check usa los datos de las pruebas de cada módulo y falla si no se alcanza el umbral. No reemplaza las pruebas de reglas negativas, concurrencia ni integración.

Los cambios se limitan a configurar el plugin existente: no se agregan librerías ni dependencias entre módulos. Al integrar las PR del equipo, hay que ejecutar nuevamente `mvn clean verify`, porque la cobertura actual no acredita la del código que todavía está en otras ramas.
