package com.uade.skillswap.domain.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void twoIdsWithTheSameValueAreEqual() {
        assertThat(new UserId("u-1")).isEqualTo(new UserId("u-1"));
    }

    @Test
    void doesNotAllowABlankId() {
        assertThatThrownBy(() -> new UserId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserId(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidTransitionTellsWhichActionAndStateFailed() {
        enum State { PENDING }

        InvalidTransitionException error = new InvalidTransitionException(State.PENDING, "pay");

        assertThat(error).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot pay in state PENDING");
        assertThat(error.getCurrentState()).isEqualTo("PENDING");
        assertThat(error.getAction()).isEqualTo("pay");
    }
}
