package com.uade.skillswap.infrastructure.persistence.rating;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.out.rating.RatingRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.InvalidRatingException;
import com.uade.skillswap.domain.rating.Rating;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class JpaRatingRepository implements RatingRepository {

    @Override
    public Rating save(Rating rating) {
        Objects.requireNonNull(rating, "rating must not be null");
        return EntityManagerContext.withEntityManager(em -> {
            RatingJpaEntity entity = RatingMapper.toEntity(rating);
            try {
                // IDENTITY triggers INSERT on persist, while other dialects/paths may defer until flush.
                em.persist(entity);
                em.flush();
            } catch (PersistenceException error) {
                if (isDuplicateBookingAuthorViolation(error)) {
                    throw new InvalidRatingException(
                            "Booking " + rating.getBookingId() + " has already been rated by user "
                                    + rating.getAuthorId().value());
                }
                throw error;
            }
            return RatingMapper.toDomain(entity);
        });
    }

    private static boolean isDuplicateBookingAuthorViolation(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                if (constraint != null && constraint.toLowerCase(Locale.ROOT).contains("uq_rating_booking_author")) {
                    return true;
                }
                if ("23505".equals(violation.getSQLState()) || violation.getErrorCode() == 1062) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean existsByBookingIdAndAuthorId(long bookingId, UserId authorId) {
        if (authorId == null) {
            return false;
        }
        return EntityManagerContext.withEntityManager(em -> {
            Long count = em.createQuery(
                            "SELECT COUNT(r.id) FROM RatingJpaEntity r WHERE r.bookingId = :bookingId AND r.authorId = :authorId",
                            Long.class)
                    .setParameter("bookingId", bookingId)
                    .setParameter("authorId", authorId.value())
                    .getSingleResult();
            return count != null && count > 0;
        });
    }

    @Override
    public Page<Rating> findByRecipientId(UserId recipientId, PageRequest pageRequest) {
        Objects.requireNonNull(recipientId, "recipientId must not be null");
        PageRequest safePage = pageRequest != null ? pageRequest : PageRequest.first();
        return EntityManagerContext.withEntityManager(em -> {
            Long total = em.createQuery(
                            "SELECT COUNT(r.id) FROM RatingJpaEntity r WHERE r.recipientId = :recipientId",
                            Long.class)
                    .setParameter("recipientId", recipientId.value())
                    .getSingleResult();
            long totalCount = total != null ? total : 0L;
            if (totalCount == 0L) {
                return Page.empty(safePage);
            }
            List<Rating> items = em.createQuery(
                            "SELECT r FROM RatingJpaEntity r WHERE r.recipientId = :recipientId ORDER BY r.ratedAt DESC, r.id DESC",
                            RatingJpaEntity.class)
                    .setParameter("recipientId", recipientId.value())
                    .setFirstResult(Math.toIntExact(safePage.offset()))
                    .setMaxResults(safePage.size())
                    .getResultList()
                    .stream()
                    .map(RatingMapper::toDomain)
                    .toList();
            return new Page<>(items, safePage, totalCount);
        });
    }
}
