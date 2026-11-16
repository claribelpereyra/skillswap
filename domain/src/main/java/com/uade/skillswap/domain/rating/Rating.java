package com.uade.skillswap.domain.rating;

import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;
import java.util.Objects;

public class Rating {

    private final Long id;
    private final long bookingId;
    private final UserId authorId;
    private final UserId recipientId;
    private final int score;
    private final String comment;
    private final Instant ratedAt;

    private Rating(Long id,
                   long bookingId,
                   UserId authorId,
                   UserId recipientId,
                   int score,
                   String comment,
                   Instant ratedAt) {
        validateInvariants(bookingId, authorId, recipientId, score);
        this.id = id;
        this.bookingId = bookingId;
        this.authorId = authorId;
        this.recipientId = recipientId;
        this.score = score;
        this.comment = comment != null ? comment.trim() : "";
        this.ratedAt = ratedAt != null ? ratedAt : Instant.now();
    }

    public static Rating create(long bookingId,
                                UserId authorId,
                                UserId recipientId,
                                int score,
                                String comment,
                                Instant ratedAt) {
        return new Rating(null, bookingId, authorId, recipientId, score, comment, ratedAt);
    }

    public static Rating create(long bookingId,
                                UserId authorId,
                                UserId recipientId,
                                int score,
                                String comment) {
        return create(bookingId, authorId, recipientId, score, comment, Instant.now());
    }

    public static Rating rebuild(Long id,
                                 long bookingId,
                                 UserId authorId,
                                 UserId recipientId,
                                 int score,
                                 String comment,
                                 Instant ratedAt) {
        Objects.requireNonNull(id, "A stored rating must have an id");
        return new Rating(id, bookingId, authorId, recipientId, score, comment, ratedAt);
    }

    private static void validateInvariants(long bookingId, UserId authorId, UserId recipientId, int score) {
        if (bookingId <= 0) {
            throw new InvalidRatingException("The rating must belong to a valid booking");
        }
        if (authorId == null) {
            throw new InvalidRatingException("The rating author cannot be null");
        }
        if (recipientId == null) {
            throw new InvalidRatingException("The rating recipient cannot be null");
        }
        if (authorId.equals(recipientId)) {
            throw new InvalidRatingException("A user cannot rate themselves");
        }
        if (score < 1 || score > 5) {
            throw new InvalidRatingException("The rating score must be between 1 and 5 (received: " + score + ")");
        }
    }

    public Long getId() {
        return id;
    }

    public long getBookingId() {
        return bookingId;
    }

    public UserId getAuthorId() {
        return authorId;
    }

    public UserId getRecipientId() {
        return recipientId;
    }

    public int getScore() {
        return score;
    }

    public String getComment() {
        return comment;
    }

    public Instant getRatedAt() {
        return ratedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rating rating = (Rating) o;
        return bookingId == rating.bookingId
                && Objects.equals(authorId, rating.authorId)
                && Objects.equals(recipientId, rating.recipientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, authorId, recipientId);
    }
}
