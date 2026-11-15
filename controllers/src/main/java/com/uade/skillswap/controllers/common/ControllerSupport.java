package com.uade.skillswap.controllers.common;

import java.util.Locale;

// Validaciones de entrada comunes de los controllers de publicaciones.
public final class ControllerSupport {

    private ControllerSupport() {
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Unknown " + field + ": " + value);
        }
    }

    public static <E extends Enum<E>> E requireEnum(Class<E> type, String value, String field) {
        E parsed = parseEnum(type, value, field);
        if (parsed == null) {
            throw new InvalidInputException("The " + field + " is required");
        }
        return parsed;
    }

    // Extiende IllegalArgumentException para que ControllerResult la devuelva como INVALID_INPUT.
    public static class InvalidInputException extends IllegalArgumentException {

        public InvalidInputException(String message) {
            super(message);
        }
    }
}
