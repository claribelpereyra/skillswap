package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.domain.publication.PublicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

// Una transición del historial. @Immutable: Hibernate nunca genera UPDATE sobre estas filas.
@Entity
@Immutable
@Table(name = "publication_status_history")
public class PublicationHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationJpaEntity publication;

    @Column(nullable = false)
    private int seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private PublicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private PublicationStatus toStatus;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    private String reason;

    protected PublicationHistoryJpaEntity() {
    }

    PublicationHistoryJpaEntity(PublicationJpaEntity publication, int seq, PublicationStatus fromStatus,
                                PublicationStatus toStatus, String actorId, Instant occurredAt, String reason) {
        this.publication = publication;
        this.seq = seq;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.occurredAt = occurredAt;
        this.reason = reason;
    }

    PublicationStatus getFromStatus() {
        return fromStatus;
    }

    PublicationStatus getToStatus() {
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
