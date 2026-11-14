# Generador del modelo UML

`model.py` describe las clases, relaciones y diagramas sacados del código; `generate.py` arma con eso:

- `docs/uml/skillswap.mdj`: el proyecto de StarUML, con los diagramas ya dibujados.
- `docs/uml/modelo.md`: los mismos diagramas en Mermaid, para revisarlos en GitHub.

```bash
python docs/uml/generador/generate.py docs/uml
```

Sirve para agregar de una vez las clases que pasa cada área. **Una vez que el `.mdj` se retoca a mano en StarUML, no hay que volver a generarlo**, porque se pisarían esos cambios. A partir de ahí, se edita solo en StarUML (lo maneja Luqui).
