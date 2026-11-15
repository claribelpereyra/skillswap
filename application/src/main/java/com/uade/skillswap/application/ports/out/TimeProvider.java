package com.uade.skillswap.application.ports.out;

import java.time.Instant;

@FunctionalInterface
public interface TimeProvider {

    Instant now();
}
