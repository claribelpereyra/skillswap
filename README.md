# SkillSwap

TP obligatorio de Procesos de Desarrollo de Software (UADE). Backend en Java 17 con Hibernate/JPA, MySQL y Flyway, sin Spring Boot.

## Requisitos

- JDK 17
- Maven 3.9+ (o el que trae IntelliJ)
- Docker, para levantar MySQL local

## Cómo correrlo

```bash
docker compose up -d     # levanta MySQL en localhost:3306
mvn verify               # compila, corre los tests y genera cobertura
```

Para ejecutar la app, corré `com.uade.skillswap.app.Main` desde IntelliJ (módulo `app`). Al arrancar aplica las migraciones de Flyway.

`Main` arma los controllers reales mediante `CompositionRoot`. El Mock UI Kit de la cátedra todavía no está en el repositorio: por ahora el arranque comprueba la conexión y composición y termina sin abrir ventanas. Para integrar el kit, conservar el `ApplicationRuntime` abierto durante la vida de la UI y pasarle `runtime.controllers()`; cerrar el runtime al salir. El contrato está en `docs/informe/105-integracion-mock-ui-kit.md`.

El recorrido de backend completo se puede ejecutar sin Docker, contra H2:

```bash
mvn -B -pl app -am -Dtest=FullJourneyIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Ese test publica, solicita, acepta, reprograma, paga, completa y califica usando los controllers y repositorios reales; también prueba cancelación, reintegro configurable, avisos a ambas partes y rollback. Para la entrega final falta validar sobre MySQL real.

La conexión se puede cambiar con variables de entorno (los defaults coinciden con el `docker-compose.yml`):

| Variable | Default |
| --- | --- |
| `SKILLSWAP_DB_URL` | `jdbc:mysql://localhost:3306/skillswap` |
| `SKILLSWAP_DB_USER` | `skillswap` |
| `SKILLSWAP_DB_PASSWORD` | `skillswap` |
| `SKILLSWAP_CANCELACION_TOTAL_HORAS` | `48` |
| `SKILLSWAP_CANCELACION_PARCIAL_HORAS` | `24` |
| `SKILLSWAP_RECOMENDACION` | `AFINIDAD` (`POPULARIDAD` como alternativa) |

Los umbrales se aplican al reintegro y la estrategia se usa cuando el formulario de recomendación no elige otra. La identidad y los canales siguen siendo simulados; los avisos se procesan después del commit, con registro de fallos y sin reintentos automáticos.

El estado de las tareas 88–107 está en `docs/informe/estado-tareas-ian.md`.

La cobertura de cada módulo queda en `<modulo>/target/site/jacoco/index.html`.

## Estructura y regla de dependencias

```
domain          entidades, value objects, reglas, estados, eventos (sin dependencias)
application     casos de uso + puertos de entrada/salida        -> domain
infrastructure  JPA, repositorios, Flyway, adaptadores mock     -> application
controllers     controllers delgados que llaman a casos de uso  -> application
app             Mock UI Kit + Main (arma todo el grafo)         -> controllers, infrastructure
```

Paquetes: `com.uade.skillswap.domain`, `com.uade.skillswap.application` (`usecases`, `ports.in`, `ports.out`, `common`), `com.uade.skillswap.infrastructure` (`persistence`, `adapters`), `com.uade.skillswap.controllers` y `com.uade.skillswap.app`.

Las dependencias solo apuntan hacia adentro. Como cada capa es un módulo Maven, si el dominio importa algo de JPA o un controller usa un repositorio, **no compila**. No agreguen dependencias entre módulos sin hablarlo con el equipo.

Convenciones:

- Código en inglés: paquetes, clases, métodos, atributos, variables, nombres de tests y mensajes de error. Comentarios y documentación en español.
- Las entidades JPA van en infraestructura (por ejemplo `PublicationJpaEntity`) y se mapean a las de dominio. El dominio no lleva anotaciones `@Entity`.
- Cada entidad JPA nueva se agrega en `META-INF/persistence.xml`.
- El esquema lo maneja Flyway (`infrastructure/src/main/resources/db/migration`). Hibernate solo valida (`hbm2ddl.auto=validate`).
- Nunca editar una migración ya mergeada: se crea una nueva `V<n>__descripcion.sql`.
- Todo caso negativo del enunciado (transición inválida, saldo insuficiente, cupo agotado, doble pago, calificación no autorizada) lleva su test.

## Documentación

Diagramas StarUML en `docs/uml/` e informe de decisiones en `docs/informe/`.
