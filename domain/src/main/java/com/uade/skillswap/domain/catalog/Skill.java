package com.uade.skillswap.domain.catalog;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// Hoja del Composite: una habilidad concreta (por ejemplo "Java" dentro de "Backend").
public final class Skill extends CatalogItem {

    public Skill(long id, String name) {
        super(id, name);
    }

    @Override
    public Set<Long> idsInSubtree() {
        return Set.of(getId());
    }

    @Override
    public List<Skill> skills() {
        return List.of(this);
    }

    @Override
    public Optional<CatalogItem> find(long id) {
        return id == getId() ? Optional.of(this) : Optional.empty();
    }
}
