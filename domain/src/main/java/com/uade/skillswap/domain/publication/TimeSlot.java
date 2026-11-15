package com.uade.skillswap.domain.publication;

import java.time.Instant;
import java.util.Objects;

public class TimeSlot {

    private final Long id;
    private final Instant startsAt;
    private final Instant endsAt;

    public TimeSlot(Long id, Instant startsAt, Instant endsAt) {
        this.id = id;
        this.startsAt = Objects.requireNonNull(startsAt, "The slot start is required");
        this.endsAt = Objects.requireNonNull(endsAt, "The slot end is required");
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("A slot must end after it starts");
        }
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
}
