# SkillSwap

TP obligatorio de Procesos de Desarrollo de Software (UADE). Backend en Java 17 con Hibernate/JPA, MySQL y Flyway, sin Spring Boot.

## Requisitos

- JDK 17
- Maven 3.9+ (o el que trae IntelliJ)
- Docker Compose, para levantar MySQL 8.4 local

## Cómo correrlo

```bash
docker compose up -d     # levanta MySQL en localhost:3306
mvn verify               # compila, corre los tests y genera cobertura
```

Para ejecutar la app, corré `com.uade.skillswap.app.Main` desde IntelliJ (módulo `app`). Al arrancar aplica las migraciones de Flyway.

El Mock UI Kit Swing está integrado con los controllers reales. `Main` arma la aplicación con CompositionRoot y abre la ventana; cerrarla libera los recursos. El build genera un JAR con sus dependencias:

```bash
mvn clean verify
java -jar app/target/app-0.1.0-SNAPSHOT.jar --demo
```

`--demo` crea datos locales reproducibles: user-mateo y user-sofia, 500 créditos iniciales por usuario, habilidades y ofertas con franjas. Reiniciar no vuelve a acreditar saldo ni duplica ofertas. El selector de sesión permite probar cliente y proveedor sin cambiar de actor automáticamente. Sin --demo no se cargan datos.

Para levantar MySQL local alcanza con `docker compose up -d` (puerto 3306). Para ejecutar con datos propios se puede pasar `--user=<userId>` al JAR y crear previamente el perfil correspondiente.

Ver `docs/informe/105-integracion-mock-ui-kit.md` para el contrato y los ajustes mínimos del kit.

El recorrido de backend completo se puede ejecutar sin Docker, contra H2:

```bash
mvn -B -pl app -am -Dtest=FullJourneyIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Ese test publica, solicita, acepta, reprograma, paga, completa y califica usando los controllers y repositorios reales; también prueba cancelación, reintegro configurable, avisos a ambas partes y rollback. También se validó contra MySQL 8.4.13 y se agregó un job MySQL 8.4 al CI. Ver `docs/informe/validacion-ui-mysql.md` para reproducirlo con bases desechables.

La conexión se puede cambiar con variables de entorno (los defaults coinciden con el `docker-compose.yml`):

| Variable | Default |
| --- | --- |
| `SKILLSWAP_DB_URL` | `jdbc:mysql://localhost:3306/skillswap` |
| `SKILLSWAP_DB_USER` | `skillswap` |
| `SKILLSWAP_DB_PASSWORD` | `skillswap` |
| `SKILLSWAP_CANCELACION_TOTAL_HORAS` | `48` |
| `SKILLSWAP_CANCELACION_PARCIAL_HORAS` | `24` |
| `SKILLSWAP_RECOMENDACION` | `AFINIDAD` (`POPULARIDAD` como alternativa) |
| `SKILLSWAP_UI_ZONE` | `America/Argentina/Buenos_Aires` |

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
