package com.uade.skillswap.domain.catalog;

import com.uade.skillswap.domain.common.BusinessRuleException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Compuesto del Composite: una categoría agrupa subcategorías y habilidades, y delega en ellas
// las operaciones del catálogo. Agregar un nivel nuevo no cambia a quien lo recorre.
public final class Category extends CatalogItem {

    private final List<CatalogItem> children = new ArrayList<>();

    public Category(long id, String name) {
        super(id, name);
    }

    public Category add(CatalogItem child) {
        if (child == this || child.includes(getId())) {
            throw new BusinessRuleException("A category cannot contain itself");
        }
        Set<Long> repeated = new HashSet<>(child.idsInSubtree());
        repeated.retainAll(idsInSubtree());
        if (!repeated.isEmpty()) {
            throw new BusinessRuleException("The catalog already contains the items " + repeated);
        }
        if (children.stream().anyMatch(child::hasSameNameAs)) {
            throw new BusinessRuleException("There is already an item named " + child.getName() + " in " + getName());
        }
        children.add(child);
        return this;
    }

    public List<CatalogItem> getChildren() {
        return List.copyOf(children);
    }

    @Override
    public Set<Long> idsInSubtree() {
        Set<Long> ids = new HashSet<>();
        ids.add(getId());
        children.forEach(child -> ids.addAll(child.idsInSubtree()));
        return Set.copyOf(ids);
    }

    @Override
    public List<Skill> skills() {
        List<Skill> skills = new ArrayList<>();
        children.forEach(child -> skills.addAll(child.skills()));
        return List.copyOf(skills);
    }

    @Override
    public Optional<CatalogItem> find(long id) {
        if (id == getId()) {
            return Optional.of(this);
        }
        return children.stream()
                .map(child -> child.find(id))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
