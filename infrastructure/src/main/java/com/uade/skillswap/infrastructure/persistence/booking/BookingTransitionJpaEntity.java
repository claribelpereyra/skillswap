package com.uade.skillswap.infrastructure.persistence.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "booking_transition")
public class BookingTransitionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingJpaEntity booking;

    @Column(nullable = false)
    private int position;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(length = 500)
    private String reason;

    protected BookingTransitionJpaEntity() {
    }

    BookingTransitionJpaEntity(BookingJpaEntity booking, int position, String fromStatus, String toStatus,
                               String actorId, Instant occurredAt, String reason) {
        this.booking = booking;
        this.position = position;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.occurredAt = occurredAt;
        this.reason = reason;
    }

    String getFromStatus() {
        return fromStatus;
    }

    String getToStatus() {
        return toStatus;
    }

    String getActorId() {
        return actorId;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    String getReason() {
        return reason;
    }
}
