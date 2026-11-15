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

- Completar la plantilla del PR.
- Necesita **1 aprobación** de otro integrante y el **CI en verde**.
- Se mergea con **Squash and merge**: un commit por PR en `develop`.
- Quien abre el PR es quien lo mergea, una vez aprobado.
- La rama se borra sola después del merge.

## Cierre de un hito: `develop` → `main`

Cuando el equipo da por terminado un hito:

1. Verificar que `develop` tiene todo lo del hito y que el CI está en verde.
2. Abrir un PR con base `main` y compare `develop`, titulado por ejemplo `Hito 1 - Diseño`.
3. Aprobarlo y mergearlo con **Create a merge commit**. Nunca con squash: si se aplasta, `main` y `develop` quedan con historiales distintos y el próximo cierre de hito trae conflictos.
4. Marcar la entrega con un tag sobre `main`:

```bash
git switch main
git pull
git tag -a hito-1 -m "Hito 1 - Diseño"
git push origin hito-1
```

Tags: `hito-1` (Diseño), `hito-2` (Núcleo), `hito-3` (Infraestructura), `hito-4` (Producto).

Si después de la entrega hay que corregir algo urgente, el fix va igual por una rama desde `develop` y entra a `main` en el próximo cierre.

## Commits

Formato: `tipo(capa): descripción en imperativo`

```
feat(dominio): agregar transiciones de estado de Publicacion
fix(aplicacion): evitar doble cobro al confirmar pago
test(infraestructura): probar optimistic locking del cupo
docs(uml): actualizar diagrama de contratación
```

Tipos: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`.

## Archivos delicados

- **UML de StarUML (`docs/uml/*.mdj`)**: Git no lo puede mergear. Una sola persona lo edita a la vez; avisen en el grupo antes de tocarlo.
- **Migraciones de Flyway**: nunca editar una ya mergeada. Si dos PRs crean la misma versión (por ejemplo dos `V3__...`), el segundo en mergear renumera la suya.
- **POMs**: agregar dependencias entre módulos se habla con el equipo antes.
