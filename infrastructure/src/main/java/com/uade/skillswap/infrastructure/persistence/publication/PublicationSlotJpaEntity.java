package com.uade.skillswap.infrastructure.persistence.publication;

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
@Table(name = "publication_slots")
public class PublicationSlotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationJpaEntity publication;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private boolean occupied;

    protected PublicationSlotJpaEntity() {
    }

    PublicationSlotJpaEntity(PublicationJpaEntity publication, Instant startsAt, Instant endsAt, boolean occupied) {
        this.publication = publication;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.occupied = occupied;
    }

    Long getId() {
        return id;
    }

    Instant getStartsAt() {
        return startsAt;
    }

    Instant getEndsAt() {
        return endsAt;
    }

    boolean isOccupied() {
        return occupied;
    }

    void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    boolean hasSchedule(Instant start, Instant end) {
        return startsAt.equals(start) && endsAt.equals(end);
    }
}
