package com.uade.skillswap.infrastructure.persistence.publication;

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

// Arma el Composite del dominio (Category/Skill) a partir de la tabla categories y le pregunta al nodo
// qué ids cuelgan de él. El catálogo es chico, así que se lee entero en una sola consulta.
public class JpaCategoryTree implements CategoryTree {

    @Override
    public Optional<Set<Long>> idsUnder(long categoryId) {
        Map<Long, CatalogItem> catalog = loadCatalog();
        return Optional.ofNullable(catalog.get(categoryId)).map(CatalogItem::idsInSubtree);
    }

    private Map<Long, CatalogItem> loadCatalog() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = EntityManagerContext.withEntityManager(em -> em.createNativeQuery(
                "SELECT id, parent_id, name, kind FROM categories ORDER BY id").getResultList());

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
