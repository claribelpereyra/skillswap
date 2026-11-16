package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.ports.out.publication.PublicationSearchQuery;
import com.uade.skillswap.application.ports.out.publication.SearchCriteria;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Búsqueda en SQL nativo con 3 consultas fijas (COUNT, página como proyección y etiquetas con IN), sin N+1.
public class JpaPublicationSearchQuery implements PublicationSearchQuery {

    // Tabla y columnas de perfiles (migración V5__profiles_and_ratings). Si cambian, se cambia solo acá.
    static final String PROFILES_TABLE = "profile";
    static final String PROFILE_USER_ID = "user_id";
    static final String PROFILE_REPUTATION = "reputation";
    static final String PROFILE_ACTIVE = "active";

    private static final String REPUTATION = "COALESCE(prof." + PROFILE_REPUTATION + ", 0)";
    private static final String TAG_MATCHES_TEXT = "EXISTS (SELECT 1 FROM publication_tag tt "
            + "WHERE tt.publication_id = pub.id AND LOWER(tt.tag) LIKE :text ESCAPE '!')";
    // Relevancia: +3 título, +2 etiqueta, +1 descripción (ver SearchSort.RELEVANCE).
    private static final String RELEVANCE = "(CASE WHEN LOWER(pub.title) LIKE :text ESCAPE '!' THEN 3 ELSE 0 END"
            + " + CASE WHEN " + TAG_MATCHES_TEXT + " THEN 2 ELSE 0 END"
            + " + CASE WHEN LOWER(pub.description) LIKE :text ESCAPE '!' THEN 1 ELSE 0 END)";

    @Override
    public Page<PublicationSummary> search(SearchCriteria criteria, PageRequest page) {
        return EntityManagerContext.withEntityManager(em -> {
            Map<String, Object> parameters = new HashMap<>();
            String from = " FROM publication pub LEFT JOIN " + PROFILES_TABLE + " prof ON prof." + PROFILE_USER_ID
                    + " = pub.author_id" + where(criteria, parameters);

            long total = ((Number) bind(em.createNativeQuery("SELECT COUNT(*)" + from), parameters)
                    .getSingleResult()).longValue();

            String select = "SELECT pub.id, pub.author_id, " + REPUTATION + " AS reputation, pub.type, pub.title,"
                    + " pub.category_id, pub.modality, pub.base_price,"
                    + " pub.capacity - (SELECT COUNT(*) FROM publication_slot s"
                    + " WHERE s.publication_id = pub.id AND s.occupied = TRUE) AS available,"
                    + " pub.published_at AS published_at, pub.description AS description, pub.duration_minutes AS duration_minutes,"
                    + " COALESCE(prof.display_name, pub.author_id) AS author_display_name"
                    + (criteria.text() == null ? "" : ", " + RELEVANCE + " AS relevance");
            Query query = bind(em.createNativeQuery(select + from + orderBy(criteria)), parameters)
                    .setFirstResult(Math.toIntExact(page.offset()))
                    .setMaxResults(page.size());
            @SuppressWarnings("unchecked")
            NativeQuery<Object[]> nativeQuery = query.unwrap(NativeQuery.class);
            nativeQuery.addScalar("id", StandardBasicTypes.LONG)
                    .addScalar("author_id", StandardBasicTypes.STRING)
                    .addScalar("reputation", StandardBasicTypes.DOUBLE)
                    .addScalar("type", StandardBasicTypes.STRING)
                    .addScalar("title", StandardBasicTypes.STRING)
                    .addScalar("category_id", StandardBasicTypes.LONG)
                    .addScalar("modality", StandardBasicTypes.STRING)
                    .addScalar("base_price", StandardBasicTypes.LONG)
                    .addScalar("available", StandardBasicTypes.INTEGER)
                    .addScalar("published_at", StandardBasicTypes.INSTANT)
                    .addScalar("description", StandardBasicTypes.STRING)
                    .addScalar("duration_minutes", StandardBasicTypes.LONG)
                    .addScalar("author_display_name", StandardBasicTypes.STRING);
            List<Object[]> rows = nativeQuery.getResultList();

            Map<Long, Set<String>> tags = tagsOf(em, rows.stream().map(r -> (Long) r[0]).toList());
            List<PublicationSummary> items = new ArrayList<>();
            for (Object[] row : rows) {
                long id = (Long) row[0];
                items.add(new PublicationSummary(id, (String) row[1], (Double) row[2],
                        PublicationType.valueOf((String) row[3]), (String) row[4], (Long) row[5],
                        row[6] == null ? null : Modality.valueOf((String) row[6]), (Long) row[7],
                        (Integer) row[8], tags.getOrDefault(id, Set.of()), (Instant) row[9],
                        (String) row[10], (Long) row[11], (String) row[12]));
            }
            return new Page<>(items, page, total);
        });
    }

    private static String where(SearchCriteria criteria, Map<String, Object> parameters) {
        // Baja lógica: las publicaciones de un perfil dado de baja no se muestran (sin perfil, se muestran).
        StringBuilder where = new StringBuilder(" WHERE pub.status = 'PUBLISHED'"
                + " AND (prof." + PROFILE_ACTIVE + " IS NULL OR prof." + PROFILE_ACTIVE + " = TRUE)");
        if (criteria.text() != null) {
            where.append(" AND (LOWER(pub.title) LIKE :text ESCAPE '!' OR LOWER(pub.description) LIKE :text ESCAPE '!'"
                    + " OR ").append(TAG_MATCHES_TEXT).append(")");
            parameters.put("text", "%" + escapeLike(criteria.text()) + "%");
        }
        if (criteria.categoryIds() != null) {
            where.append(" AND pub.category_id IN (:categories)");
            parameters.put("categories", criteria.categoryIds());
        }
        if (criteria.modality() != null) {
            where.append(" AND pub.modality = :modality");
            parameters.put("modality", criteria.modality().name());
        }
        if (criteria.minPrice() != null) {
            where.append(" AND pub.base_price >= :minPrice");
            parameters.put("minPrice", criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            where.append(" AND pub.base_price <= :maxPrice");
            parameters.put("maxPrice", criteria.maxPrice());
        }
        if (criteria.tag() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM publication_tag st"
                    + " WHERE st.publication_id = pub.id AND LOWER(st.tag) = :tag)");
            parameters.put("tag", criteria.tag());
        }
        if (criteria.minReputation() != null) {
            where.append(" AND ").append(REPUTATION).append(" >= :minReputation");
            parameters.put("minReputation", criteria.minReputation());
        }
        return where.toString();
    }

    // El id al final hace el orden determinístico, así la paginación no repite ni saltea filas.
    private static String orderBy(SearchCriteria criteria) {
        return switch (criteria.sort()) {
            case RELEVANCE -> " ORDER BY relevance DESC, reputation DESC, pub.published_at DESC, pub.id DESC";
            case BEST_REPUTATION -> " ORDER BY reputation DESC, pub.published_at DESC, pub.id DESC";
            case LOWEST_PRICE -> " ORDER BY pub.base_price ASC, pub.published_at DESC, pub.id DESC";
            case MOST_RECENT -> " ORDER BY pub.published_at DESC, pub.id DESC";
        };
    }

    private static Map<Long, Set<String>> tagsOf(EntityManager em, List<Long> ids) {
        Map<Long, Set<String>> tags = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return tags;
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT publication_id, tag FROM publication_tag WHERE publication_id IN (:ids)")
                .setParameter("ids", ids)
                .getResultList();
        for (Object[] row : rows) {
            tags.computeIfAbsent(((Number) row[0]).longValue(), id -> new HashSet<>()).add((String) row[1]);
        }
        return tags;
    }

    private static Query bind(Query query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
        return query;
    }

    // Lo que escribe el usuario se busca literal: % y _ no actúan como comodines.
    private static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
