package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.application.ports.out.publication.RecommendationMetadataQuery;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class JpaRecommendationMetadataQuery implements RecommendationMetadataQuery {

    private static final String PROFILE_TABLE = "profile";
    private static final String PROFILE_ID = "id";
    private static final String PROFILE_USER_ID = "user_id";
    private static final String PROFILE_ACTIVE = "active";
    private static final String PROFILE_SKILLS_TABLE = "profile_skill";
    private static final String PROFILE_INTERESTS_TABLE = "profile_interest";
    private static final String PROFILE_ID_COLUMN = "profile_id";
    private static final String PROFILE_SKILL_COLUMN = "skill_id";
    private static final String PROFILE_INTEREST_COLUMN = "interest";
    private static final String SKILL_CATALOG_TABLE = "category";

    @Override
    public Set<String> skillsAndInterestsFor(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }
        return EntityManagerContext.withEntityManager(em -> {
            String profileJoin = " JOIN " + PROFILE_TABLE + " profile ON profile." + PROFILE_ID
                    + " = profile_value." + PROFILE_ID_COLUMN + " WHERE profile."
                    + PROFILE_USER_ID + " = :userId AND profile." + PROFILE_ACTIVE + " = TRUE";
            String skills = "SELECT LOWER(skill.name) FROM " + PROFILE_SKILLS_TABLE + " profile_value"
                    + " JOIN " + SKILL_CATALOG_TABLE + " skill ON skill.id = profile_value."
                    + PROFILE_SKILL_COLUMN + " AND skill.kind = 'SKILL'" + profileJoin;
            String interests = "SELECT LOWER(profile_value." + PROFILE_INTEREST_COLUMN + ") FROM "
                    + PROFILE_INTERESTS_TABLE + " profile_value" + profileJoin;
            List<?> rows = em.createNativeQuery(skills + " UNION " + interests)
                    .setParameter("userId", userId)
                    .getResultList();
            return rows.stream()
                    .map(Object::toString)
                    .map(String::strip)
                    .filter(value -> !value.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        });
    }

    @Override
    public Map<Long, Long> recentBookingCounts(Set<Long> publicationIds, Instant since) {
        Objects.requireNonNull(publicationIds, "The publication identifiers are required");
        Objects.requireNonNull(since, "The start of the popularity window is required");
        if (publicationIds.isEmpty()) {
            return Map.of();
        }
        return EntityManagerContext.withEntityManager(em -> countsFor(em, publicationIds, since));
    }

    private static Map<Long, Long> countsFor(EntityManager em, Set<Long> publicationIds, Instant since) {
        String query = "SELECT booking.publication_id, COUNT(DISTINCT booking.id) "
                + "FROM booking booking JOIN booking_transition transition "
                + "ON transition.booking_id = booking.id "
                + "WHERE booking.publication_id IN (:publicationIds) "
                + "AND transition.from_status IS NULL AND transition.occurred_at >= :since "
                + "AND booking.status IN ('ACCEPTED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED') "
                + "GROUP BY booking.publication_id";
        List<?> rows = em.createNativeQuery(query)
                .setParameter("publicationIds", publicationIds)
                .setParameter("since", since)
                .getResultList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            counts.put(((Number) columns[0]).longValue(), ((Number) columns[1]).longValue());
        }
        return Map.copyOf(counts);
    }
}
