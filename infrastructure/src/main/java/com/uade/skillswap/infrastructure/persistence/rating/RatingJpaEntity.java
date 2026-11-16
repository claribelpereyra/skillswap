package com.uade.skillswap.infrastructure.persistence.rating;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "rating")
public class RatingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private long bookingId;

    @Column(name = "author_id", nullable = false, length = 100)
    private String authorId;

    @Column(name = "recipient_id", nullable = false, length = 100)
    private String recipientId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "comment", nullable = false, length = 1000)
    private String comment;

    @Column(name = "rated_at", nullable = false)
    private Instant ratedAt;

    protected RatingJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getRatedAt() {
        return ratedAt;
    }

    public void setRatedAt(Instant ratedAt) {
        this.ratedAt = ratedAt;
    }
}
