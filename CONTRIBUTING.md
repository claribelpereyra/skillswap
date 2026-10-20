# Cómo trabajamos en SkillSwap

## Branches

| Rama | Qué contiene | Quién escribe |
| --- | --- | --- |
| `develop` | Integración del día a día. Es la rama **default** del repo. | PRs desde ramas de trabajo |
| `main` | Solo lo que se entregó en cada hito. | Únicamente PRs desde `develop` al cerrar un hito |
| ramas de trabajo | Una tarea concreta. | Quien la crea |

**Nadie pushea directo a `develop` ni a `main`.** Las dos están protegidas: se cambian solo por Pull Request, con 1 aprobación y el CI en verde.

Nombres de las ramas de trabajo:

| Prefijo | Para qué | Ejemplo |
| --- | --- | --- |
| `feature/` | funcionalidad nueva | `feature/estados-publicacion` |
| `fix/` | corrección de un bug | `fix/doble-cobro-pago` |
| `test/` | solo tests | `test/casos-negativos-cupo` |
| `refactor/` | cambio interno sin cambiar comportamiento | `refactor/extraer-politica-cancelacion` |
| `docs/` | UML, informe, README | `docs/diagrama-hito-1` |

Ramas chicas y de vida corta (idealmente 1 a 3 días). Si una rama crece mucho, partila.

## Flujo del día a día

Las ramas de trabajo **salen de `develop` y vuelven a `develop`**.

```bash
git switch develop
git pull
git switch -c feature/estados-publicacion
# ... trabajar, commitear ...
git push -u origin feature/estados-publicacion
# abrir el PR en GitHub contra develop (es la base por defecto)
```

Antes de pedir review, traé lo último de `develop` para resolver conflictos de tu lado:

```bash
git fetch origin
git rebase origin/develop     # o git merge origin/develop si preferís
```

## Pull Requests a `develop`

- **Título en inglés**, con el formato de los commits (por ejemplo `feat(dominio): add Publicacion state transitions`). Con squash, el título del PR queda como el commit en `develop`.
- **Descripción en español**, completando la plantilla del PR.
- Necesita **1 aprobación** de otro integrante y el **CI en verde**.
- Se mergea con **Squash and merge**: un commit por PR en `develop`.
- Quien abre el PR es quien lo mergea, una vez aprobado.
- La rama se borra sola después del merge.

## Cierre de un hito: `develop` → `main`

Cuando el equipo da por terminado un hito:

1. Verificar que `develop` tiene todo lo del hito y que el CI está en verde.
2. Abrir un PR con base `main` y compare `develop`
3. Aprobarlo y mergearlo con **Create a merge commit**. Nunca con squash: si se aplasta, `main` y `develop` quedan con historiales distintos y el próximo cierre de hito trae conflictos.
4. Marcar la entrega con un tag sobre `main`:

## Commits

Formato: `tipo(capa): descripción en imperativo`, en inglés. Los commits de `develop` salen de los títulos de los PRs, así que siguen la misma regla.

```
feat(dominio): add Publicacion state transitions
fix(aplicacion): prevent double charge when confirming payment
test(infraestructura): cover optimistic locking on cupo
docs(uml): update contratacion diagram
```

Tipos: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`.