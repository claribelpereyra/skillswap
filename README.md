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

Para ejecutar la app, corré `com.uade.skillswap.app.Main` desde IntelliJ (módulo `skillswap-app`). Al arrancar aplica las migraciones de Flyway.

La conexión se puede cambiar con variables de entorno (los defaults coinciden con el `docker-compose.yml`):

| Variable | Default |
| --- | --- |
| `SKILLSWAP_DB_URL` | `jdbc:mysql://localhost:3306/skillswap` |
| `SKILLSWAP_DB_USER` | `skillswap` |
| `SKILLSWAP_DB_PASSWORD` | `skillswap` |

La cobertura de cada módulo queda en `<modulo>/target/site/jacoco/index.html`.

## Estructura y regla de dependencias

```
skillswap-dominio          entidades, value objects, reglas, estados, eventos (sin dependencias)
skillswap-aplicacion       casos de uso + puertos de entrada/salida      -> dominio
skillswap-infraestructura  JPA, repositorios, Flyway, adaptadores mock   -> aplicacion
skillswap-controllers      controllers delgados que llaman a casos de uso -> aplicacion
skillswap-app              Mock UI Kit + Main (arma todo el grafo)       -> controllers, infraestructura
```

Las dependencias solo apuntan hacia adentro. Como cada capa es un módulo Maven, si el dominio importa algo de JPA o un controller usa un repositorio, **no compila**. No agreguen dependencias entre módulos sin hablarlo con el equipo.

Convenciones:

- Las entidades JPA van en infraestructura (por ejemplo `PublicacionJpaEntity`) y se mapean a las de dominio. El dominio no lleva anotaciones `@Entity`.
- Cada entidad JPA nueva se agrega en `META-INF/persistence.xml`.
- El esquema lo maneja Flyway (`skillswap-infraestructura/src/main/resources/db/migration`). Hibernate solo valida (`hbm2ddl.auto=validate`).
- Nunca editar una migración ya mergeada: se crea una nueva `V<n>__descripcion.sql`.
- Todo caso negativo del enunciado (transición inválida, saldo insuficiente, cupo agotado, doble pago, calificación no autorizada) lleva su test.

## Documentación

Diagramas StarUML en `docs/uml/` e informe de decisiones en `docs/informe/`.
