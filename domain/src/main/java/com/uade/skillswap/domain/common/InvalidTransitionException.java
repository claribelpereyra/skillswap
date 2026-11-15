package com.uade.skillswap.domain.common;

public class InvalidTransitionException extends BusinessRuleException {

    private final String currentState;
    private final String action;

    public InvalidTransitionException(Enum<?> currentState, String action) {
        super("Cannot " + action + " in state " + currentState.name());
        this.currentState = currentState.name();
        this.action = action;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getAction() {
        return action;
    }
}
