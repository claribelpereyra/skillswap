package com.uade.skillswap.application.ports.out;

import java.util.function.Supplier;

@FunctionalInterface
public interface UnitOfWork {

    <T> T execute(Supplier<T> work);

    default void execute(Runnable work) {
        execute(() -> {
            work.run();
            return null;
        });
    }
}
