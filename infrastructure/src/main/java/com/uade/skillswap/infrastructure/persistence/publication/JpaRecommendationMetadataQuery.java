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

public final class JpaRecommendationMetadataQuery implements RecommendationMetadataQuery {

    @Override
    public Set<String> skillsAndInterestsFor(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }
        return EntityManagerContext.withEntityManager(em -> {
            String profileJoin = " JOIN " + JpaPublicationSearchQuery.PROFILES_TABLE + " profile ON profile."
                    + JpaPublicationSearchQuery.PROFILE_ID + " = profile_value."
                    + JpaPublicationSearchQuery.PROFILE_ID_COLUMN + " WHERE profile."
                    + JpaPublicationSearchQuery.PROFILE_USER_ID + " = :userId AND profile."
                    + JpaPublicationSearchQuery.PROFILE_ACTIVE + " = TRUE";
            String skills = "SELECT LOWER(profile_value." + JpaPublicationSearchQuery.PROFILE_SKILL_COLUMN + ") FROM "
                    + JpaPublicationSearchQuery.PROFILE_SKILLS_TABLE + " profile_value" + profileJoin;
            String interests = "SELECT LOWER(profile_value." + JpaPublicationSearchQuery.PROFILE_INTEREST_COLUMN + ") FROM "
                    + JpaPublicationSearchQuery.PROFILE_INTERESTS_TABLE + " profile_value" + profileJoin;
            List<?> rows = em.createNativeQuery(skills + " UNION " + interests)
                    .setParameter("userId", userId)
                    .getResultList();
            return rows.stream()
                    .map(Object::toString)
                    .map(String::strip)
                    .filter(value -> !value.isEmpty())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
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
