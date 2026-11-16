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

    // Permite registrar un efecto que debe ocurrir recién cuando la transacción raíz confirma: si
    // la transacción se rollbackea, el callback no se ejecuta. La implementación por defecto corre
    // el callback de inmediato, pensada para tests que no abren una transacción real.
    default void afterCommit(Runnable callback) {
        callback.run();
    }
}
