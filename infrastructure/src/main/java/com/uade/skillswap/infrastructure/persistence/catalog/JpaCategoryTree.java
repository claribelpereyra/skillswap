package com.uade.skillswap.infrastructure.persistence.catalog;

import com.uade.skillswap.application.ports.out.publication.CatalogRepository;
import com.uade.skillswap.application.ports.out.publication.CategoryTree;
import com.uade.skillswap.domain.catalog.CatalogItem;
import com.uade.skillswap.domain.catalog.Category;
import com.uade.skillswap.domain.catalog.Skill;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Arma el Composite (Category/Skill) leyendo el catálogo entero en una consulta y le pide al nodo sus ids.
public class JpaCategoryTree implements CategoryTree, CatalogRepository {

    @Override
    public Optional<Set<Long>> idsUnder(long categoryId) {
        Map<Long, CatalogItem> catalog = loadCatalog();
        return Optional.ofNullable(catalog.get(categoryId)).map(CatalogItem::idsInSubtree);
    }

    @Override
    public boolean contains(long categoryId) {
        return EntityManagerContext.withEntityManager(em -> ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM category WHERE id = :id").setParameter("id", categoryId)
                .getSingleResult()).longValue() > 0);
    }

    @Override
    public Map<Long, CatalogItem> loadAll() {
        return loadCatalog();
    }

    @Override
    public long nextId() {
        return EntityManagerContext.withEntityManager(em -> ((Number) em.createNativeQuery(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM category").getSingleResult()).longValue());
    }

    @Override
    public void add(Long parentId, CatalogItem item) {
        EntityManagerContext.withEntityManager(em -> em.createNativeQuery(
                        "INSERT INTO category (id, parent_id, name, kind) VALUES (:id, :parent, :name, :kind)")
                .setParameter("id", item.getId())
                .setParameter("parent", parentId)
                .setParameter("name", item.getName())
                .setParameter("kind", item instanceof Skill ? "SKILL" : "CATEGORY")
                .executeUpdate());
    }

    private Map<Long, CatalogItem> loadCatalog() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = EntityManagerContext.withEntityManager(em -> em.createNativeQuery(
                "SELECT id, parent_id, name, kind FROM category ORDER BY id").getResultList());

        Map<Long, CatalogItem> items = new HashMap<>();
        for (Object[] row : rows) {
            long id = ((Number) row[0]).longValue();
            String name = (String) row[2];
            items.put(id, "SKILL".equals(row[3]) ? new Skill(id, name) : new Category(id, name));
        }
        for (Object[] row : rows) {
            if (row[1] == null) {
                continue;
            }
            CatalogItem parent = items.get(((Number) row[1]).longValue());
            if (!(parent instanceof Category category)) {
                throw new IllegalStateException("Catalog item " + row[0] + " hangs from a skill");
            }
            category.add(items.get(((Number) row[0]).longValue()));
        }
        return items;
    }
}
