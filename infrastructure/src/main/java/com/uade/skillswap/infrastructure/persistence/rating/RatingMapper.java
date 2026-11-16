package com.uade.skillswap.infrastructure.persistence.rating;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.Rating;

final class RatingMapper {

    private RatingMapper() {
    }

    static RatingJpaEntity toEntity(Rating rating) {
        RatingJpaEntity entity = new RatingJpaEntity();
        entity.setBookingId(rating.getBookingId());
        entity.setAuthorId(rating.getAuthorId().value());
        entity.setRecipientId(rating.getRecipientId().value());
        entity.setScore(rating.getScore());
        entity.setComment(rating.getComment());
        entity.setRatedAt(rating.getRatedAt());
        return entity;
    }

    static Rating toDomain(RatingJpaEntity entity) {
        return Rating.rebuild(
                entity.getId(),
                entity.getBookingId(),
                new UserId(entity.getAuthorId()),
                new UserId(entity.getRecipientId()),
                entity.getScore(),
                entity.getComment(),
                entity.getRatedAt()
        );
    }
}
