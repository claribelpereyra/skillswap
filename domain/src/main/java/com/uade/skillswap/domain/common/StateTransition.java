package com.uade.skillswap.domain.common;

import java.time.Instant;
import java.util.Objects;

public record StateTransition<S extends Enum<S>>(S from, S to, UserId actor, Instant occurredAt, String reason) {

    public StateTransition {
        Objects.requireNonNull(to, "The new state is required");
        Objects.requireNonNull(actor, "The actor is required");
        Objects.requireNonNull(occurredAt, "The date is required");
        if (reason != null && reason.isBlank()) {
            reason = null;
        }
    }
}
