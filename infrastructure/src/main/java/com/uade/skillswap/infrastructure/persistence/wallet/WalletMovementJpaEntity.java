package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.domain.wallet.CreditMovementType;
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

import java.time.Instant;

@Entity
@Table(name = "wallet_movement")
public class WalletMovementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_owner_id", nullable = false)
    private WalletJpaEntity wallet;

    @Column(name = "seq", nullable = false)
    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private CreditMovementType movementType;

    @Column(nullable = false)
    private long amount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "booking_id")
    private Long bookingId;

    protected WalletMovementJpaEntity() {
    }

    WalletMovementJpaEntity(
            WalletJpaEntity wallet,
            int sequenceNumber,
            CreditMovementType movementType,
            long amount,
            Instant occurredAt,
            Long bookingId) {
        this.wallet = wallet;
        this.sequenceNumber = sequenceNumber;
        this.movementType = movementType;
        this.amount = amount;
        this.occurredAt = occurredAt;
        this.bookingId = bookingId;
    }

    CreditMovementType getMovementType() {
        return movementType;
    }

    long getAmount() {
        return amount;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    int getSequenceNumber() {
        return sequenceNumber;
    }

    Long getBookingId() {
        return bookingId;
    }
}
