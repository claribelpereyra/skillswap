package com.uade.skillswap.domain.publication;

import java.time.Instant;
import java.util.Objects;

// Franja de disponibilidad de una publicación. Solo la publicación la ocupa o la libera.
public class TimeSlot {

    private final Long id;
    private final Instant startsAt;
    private final Instant endsAt;
    private boolean occupied;

    public TimeSlot(Long id, Instant startsAt, Instant endsAt) {
        this(id, startsAt, endsAt, false);
    }

    public TimeSlot(Long id, Instant startsAt, Instant endsAt, boolean occupied) {
        this.id = id;
        this.startsAt = Objects.requireNonNull(startsAt, "The slot start is required");
        this.endsAt = Objects.requireNonNull(endsAt, "The slot end is required");
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("A slot must end after it starts");
        }
        this.occupied = occupied;
    }

    public boolean overlaps(TimeSlot other) {
        return startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt);
    }

    void occupy() {
        occupied = true;
    }

    void release() {
        occupied = false;
    }

    public Long getId() {
        return id;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public boolean isOccupied() {
        return occupied;
    }

    // Dos franjas son la misma si cubren el mismo horario: una publicación no admite franjas repetidas.
    @Override
    public boolean equals(Object o) {
        return o instanceof TimeSlot other && startsAt.equals(other.startsAt) && endsAt.equals(other.endsAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startsAt, endsAt);
    }
}
