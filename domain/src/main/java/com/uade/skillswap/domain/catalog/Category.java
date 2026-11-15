package com.uade.skillswap.domain.catalog;

import com.uade.skillswap.domain.common.BusinessRuleException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Compuesto del Composite: agrupa subcategorías y habilidades y les delega las operaciones.
public final class Category extends CatalogItem {

    private final List<CatalogItem> children = new ArrayList<>();

    public Category(long id, String name) {
        super(id, name);
    }

    // Valida contra el árbol entero (desde la raíz), no solo contra esta rama.
    public Category add(CatalogItem child) {
        if (child == this || child.includes(getId())) {
            throw new BusinessRuleException("A category cannot contain itself");
        }
        if (child.getParent() != null) {
            throw new BusinessRuleException(child.getName() + " already belongs to " + child.getParent().getName());
        }
        Set<Long> repeated = new HashSet<>(child.idsInSubtree());
        repeated.retainAll(root().idsInSubtree());
        if (!repeated.isEmpty()) {
            throw new BusinessRuleException("The catalog already contains the items " + repeated);
        }
        if (children.stream().anyMatch(child::hasSameNameAs)) {
            throw new BusinessRuleException("There is already an item named " + child.getName() + " in " + getName());
        }
        children.add(child);
        child.attachTo(this);
        return this;
    }

    private Category root() {
        Category root = this;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
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
