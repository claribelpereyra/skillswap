package com.uade.skillswap.application.ports.out;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitOfWorkTest {

    private final UnitOfWork direct = Supplier::get;

    @Test
    void returnsTheResultOfTheWork() {
        assertThat(direct.execute(() -> 42)).isEqualTo(42);
    }

    @Test
    void runsWorkWithoutResult() {
        List<String> done = new ArrayList<>();

        direct.execute(() -> {
            done.add("ok");
        });

        assertThat(done).containsExactly("ok");
    }

    @Test
    void letsTheExceptionOfTheWorkThrough() {
        assertThatThrownBy(() -> direct.execute(() -> {
            throw new IllegalStateException("failed");
        })).isInstanceOf(IllegalStateException.class).hasMessage("failed");
    }
}
