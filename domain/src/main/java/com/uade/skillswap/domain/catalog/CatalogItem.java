package com.uade.skillswap.domain.catalog;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// Composite: categoría (compuesto) y habilidad (hoja) se recorren igual, sin instanceof.
public abstract class CatalogItem {

    private final long id;
    private final String name;
    private Category parent;

    protected CatalogItem(long id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A catalog item needs a name");
        }
        this.id = id;
        this.name = name.strip();
    }

    public abstract Set<Long> idsInSubtree();

    public abstract List<Skill> skills();

    public abstract Optional<CatalogItem> find(long id);

    public boolean includes(long id) {
        return find(id).isPresent();
    }

    public Category getParent() {
        return parent;
    }

    void attachTo(Category newParent) {
        this.parent = newParent;
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
    public boolean equals(Object object) {
        return object instanceof CatalogItem other && id == other.id;
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
