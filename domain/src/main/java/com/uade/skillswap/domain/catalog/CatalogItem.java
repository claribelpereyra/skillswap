package com.uade.skillswap.domain.catalog;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// Patrón Composite: componente común del catálogo. Una categoría (compuesto) y una habilidad (hoja)
// se recorren igual, así la búsqueda y la recomendación no necesitan saber qué tan profundo es el árbol
// ni preguntar con instanceof qué tipo de nodo tienen.
public abstract class CatalogItem {

    private final long id;
    private final String name;

    protected CatalogItem(long id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A catalog item needs a name");
        }
        this.id = id;
        this.name = name.strip();
    }

    // Ids de este nodo y de todo lo que cuelga de él. Buscar por una categoría incluye sus subcategorías
    // y sus habilidades.
    public abstract Set<Long> idsInSubtree();

    // Habilidades de este nodo y de todo lo que cuelga de él.
    public abstract List<Skill> skills();

    // Busca un nodo por id en este subárbol.
    public abstract Optional<CatalogItem> find(long id);

    public boolean includes(long id) {
        return find(id).isPresent();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    boolean hasSameNameAs(CatalogItem other) {
        return name.equalsIgnoreCase(other.name);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CatalogItem other && id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
