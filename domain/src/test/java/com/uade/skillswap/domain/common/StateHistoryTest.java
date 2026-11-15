package com.uade.skillswap.domain.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StateHistoryTest {

    private enum State { DRAFT, PUBLISHED, PAUSED }

    private static final UserId AUTHOR = new UserId("author-1");
    private static final Instant CREATED_AT = Instant.parse("2026-10-20T10:00:00Z");

    @Test
    void startsWithTheCreationInTheInitialState() {
        StateHistory<State> history = StateHistory.startingAt(State.DRAFT, AUTHOR, CREATED_AT);

        assertThat(history.currentState()).isEqualTo(State.DRAFT);
        assertThat(history.transitions()).singleElement().satisfies(creation -> {
            assertThat(creation.from()).isNull();
            assertThat(creation.actor()).isEqualTo(AUTHOR);
            assertThat(creation.occurredAt()).isEqualTo(CREATED_AT);
        });
    }

    @Test
    void recordsDateActorAndReasonOfEachTransition() {
        StateHistory<State> history = StateHistory.startingAt(State.DRAFT, AUTHOR, CREATED_AT);
        Instant later = CREATED_AT.plusSeconds(60);

        history.record(State.DRAFT, State.PUBLISHED, AUTHOR, later, null);
        history.record(State.PUBLISHED, State.PAUSED, AUTHOR, later, "On vacation");

        assertThat(history.currentState()).isEqualTo(State.PAUSED);
        assertThat(history.last().reason()).isEqualTo("On vacation");
        assertThat(history.transitions()).extracting(StateTransition::to)
                .containsExactly(State.DRAFT, State.PUBLISHED, State.PAUSED);
    }

    @Test
    void doesNotAllowChangingTheRecordsFromOutside() {
        StateHistory<State> history = StateHistory.startingAt(State.DRAFT, AUTHOR, CREATED_AT);

        List<StateTransition<State>> copy = history.transitions();

        assertThatThrownBy(copy::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(history.transitions()).hasSize(1);
    }

    @Test
    void rejectsATransitionThatDoesNotStartFromTheCurrentState() {
        StateHistory<State> history = StateHistory.startingAt(State.DRAFT, AUTHOR, CREATED_AT);

        assertThatThrownBy(() -> history.record(State.PUBLISHED, State.PAUSED, AUTHOR, CREATED_AT, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("the current state is DRAFT");
    }

    @Test
    void rejectsADateBeforeTheLastRecordedOne() {
        StateHistory<State> history = StateHistory.startingAt(State.DRAFT, AUTHOR, CREATED_AT);

        assertThatThrownBy(() -> history.record(
                State.DRAFT, State.PUBLISHED, AUTHOR, CREATED_AT.minusSeconds(1), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void treatsABlankReasonAsNoReason() {
        StateTransition<State> transition =
                new StateTransition<>(State.DRAFT, State.PUBLISHED, AUTHOR, CREATED_AT, "   ");

        assertThat(transition.reason()).isNull();
    }

    @Test
    void findsTheLastTimeAStateWasReached() {
        StateHistory<State> history = StateHistory.startingAt(State.DRAFT, AUTHOR, CREATED_AT);
        history.record(State.DRAFT, State.PUBLISHED, AUTHOR, CREATED_AT.plusSeconds(10), null);
        history.record(State.PUBLISHED, State.PAUSED, AUTHOR, CREATED_AT.plusSeconds(20), null);
        history.record(State.PAUSED, State.PUBLISHED, AUTHOR, CREATED_AT.plusSeconds(30), null);

        assertThat(history.lastTransitionTo(State.PUBLISHED)).get()
                .extracting(StateTransition::occurredAt).isEqualTo(CREATED_AT.plusSeconds(30));
        assertThat(history.lastTransitionTo(State.PAUSED)).isPresent();
    }

    @Test
    void rebuildsAStoredHistoryValidatingTheChain() {
        List<StateTransition<State>> stored = List.of(
                new StateTransition<>(null, State.DRAFT, AUTHOR, CREATED_AT, null),
                new StateTransition<>(State.DRAFT, State.PUBLISHED, AUTHOR, CREATED_AT.plusSeconds(5), null));

        assertThat(StateHistory.rebuild(stored).currentState()).isEqualTo(State.PUBLISHED);
    }

    @Test
    void doesNotRebuildAHistoryThatDoesNotStartWithTheCreation() {
        List<StateTransition<State>> stored = List.of(
                new StateTransition<>(State.DRAFT, State.PUBLISHED, AUTHOR, CREATED_AT, null));

        assertThatThrownBy(() -> StateHistory.rebuild(stored))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
