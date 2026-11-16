package com.uade.skillswap.infrastructure.persistence.catalog;

import com.uade.skillswap.application.common.*;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;
import com.uade.skillswap.application.ports.out.publication.CatalogQuery;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import java.util.List;

public final class JpaCatalogQuery implements CatalogQuery {
    @Override public Page<CatalogEntryView> list(PageRequest page) {
        return EntityManagerContext.withEntityManager(em -> {
            long total = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM category").getSingleResult()).longValue();
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery("SELECT id,name,kind FROM category ORDER BY id")
                    .setFirstResult(Math.toIntExact(page.offset())).setMaxResults(page.size()).getResultList();
            return new Page<>(rows.stream().map(row -> new CatalogEntryView(((Number) row[0]).longValue(),
                    (String) row[1], (String) row[2])).toList(), page, total);
        });
    }
}
