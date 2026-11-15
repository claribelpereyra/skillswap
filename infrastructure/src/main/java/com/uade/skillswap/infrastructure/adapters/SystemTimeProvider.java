package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.application.ports.out.TimeProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class SystemTimeProvider implements TimeProvider {

    private final Clock clock;

    public SystemTimeProvider() {
        this(Clock.systemUTC());
    }

    public SystemTimeProvider(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
