package com.uade.skillswap.infrastructure.persistence.catalog;

import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import com.uade.skillswap.infrastructure.persistence.JpaUnitOfWork;
import java.util.List;

/** Datos explícitos de demostración: se invoca solo al solicitar --demo. */
public final class DemoCatalogData {
    public record Catalog(long categoryId, long skillId) { }
    private DemoCatalogData() { }
    public static Catalog ensure() {
        return new JpaUnitOfWork().execute(() -> {
            long category = ensureNode(null, "Tecnología", "CATEGORY");
            return new Catalog(category, ensureNode(category, "Java", "SKILL"));
        });
    }
    private static long ensureNode(Long parent, String name, String kind) {
        return EntityManagerContext.withEntityManager(em -> {
            @SuppressWarnings("unchecked")
            List<Number> ids = em.createNativeQuery("SELECT id FROM category WHERE name=:name AND parent_key=:parent")
                    .setParameter("name",name).setParameter("parent",parent == null ? 0L : parent).getResultList();
            if (!ids.isEmpty()) return ids.get(0).longValue();
            long id = ((Number)em.createNativeQuery("SELECT COALESCE(MAX(id),0)+1 FROM category").getSingleResult()).longValue();
            em.createNativeQuery("INSERT INTO category(id,parent_id,name,kind) VALUES(:id,:parent,:name,:kind)")
                    .setParameter("id",id).setParameter("parent",parent).setParameter("name",name).setParameter("kind",kind)
                    .executeUpdate();
            return id;
        });
    }
}
