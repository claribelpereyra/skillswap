package com.uade.skillswap.application.dto.rating;

import com.uade.skillswap.domain.rating.Rating;

import java.time.Instant;

public record RatingView(
        Long id,
        long bookingId,
        String authorId,
        String recipientId,
        int score,
        String comment,
        Instant ratedAt) {

    public static RatingView from(Rating rating) {
        return new RatingView(
                rating.getId(),
                rating.getBookingId(),
                rating.getAuthorId().value(),
                rating.getRecipientId().value(),
                rating.getScore(),
                rating.getComment(),
                rating.getRatedAt()
        );
    }
}
