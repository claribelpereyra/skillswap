package com.uade.skillswap.domain.common;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StateHistory<S extends Enum<S>> {

    private final List<StateTransition<S>> transitions = new ArrayList<>();

    public static <S extends Enum<S>> StateHistory<S> startingAt(S initialState, UserId actor, Instant occurredAt) {
        StateHistory<S> history = new StateHistory<>();
        history.transitions.add(new StateTransition<>(null, initialState, actor, occurredAt, null));
        return history;
    }

    public static <S extends Enum<S>> StateHistory<S> rebuild(List<StateTransition<S>> stored) {
        if (stored.isEmpty() || stored.get(0).from() != null) {
            throw new IllegalArgumentException("A state history must start with the creation of the entity");
        }
        StateHistory<S> history = new StateHistory<>();
        history.transitions.add(stored.get(0));
        stored.stream().skip(1).forEach(history::append);
        return history;
    }

    private StateHistory() {
    }

    public void record(S from, S to, UserId actor, Instant occurredAt, String reason) {
        append(new StateTransition<>(from, to, actor, occurredAt, reason));
    }

    private void append(StateTransition<S> next) {
        StateTransition<S> last = last();
        if (next.from() != last.to()) {
            throw new IllegalStateException(
                    "The transition starts from " + next.from() + " but the current state is " + last.to());
        }
        if (next.occurredAt().isBefore(last.occurredAt())) {
            throw new IllegalStateException("A transition cannot be dated before the last recorded one");
        }
        transitions.add(next);
    }

    public S currentState() {
        return last().to();
    }

    public StateTransition<S> last() {
        return transitions.get(transitions.size() - 1);
    }

    public List<StateTransition<S>> transitions() {
        return List.copyOf(transitions);
    }

    public Optional<StateTransition<S>> lastTransitionTo(S state) {
        for (int i = transitions.size() - 1; i >= 0; i--) {
            if (transitions.get(i).to() == state) {
                return Optional.of(transitions.get(i));
            }
        }
        return Optional.empty();
    }
}
