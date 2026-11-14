package com.uade.skillswap.controllers.common;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.PermissionDeniedException;
import com.uade.skillswap.domain.common.UserId;

import java.util.Locale;
import java.util.function.Supplier;

// Ayudas comunes de los controllers: traducir excepciones a un Result uniforme y validar la entrada.
public final class ControllerSupport {

    private ControllerSupport() {
    }

    // Corre la operación y traduce cualquier error de aplicación al mismo formato.
    // El orden importa: PermissionDeniedException e InvalidTransitionException son BusinessRuleException
    // más específicas, así que van antes.
    public static <T> Result<T> handle(Supplier<T> operation) {
        try {
            return Result.ok(operation.get());
        } catch (InvalidInputException | IllegalArgumentException e) {
            // Los nulos que pueden venir de la UI se validan antes con InvalidInputException. Un
            // NullPointerException es un bug interno: cae en UNEXPECTED y no se muestra el detalle.
            return Result.fail(ErrorCode.INVALID_INPUT, e.getMessage());
        } catch (ResourceNotFoundException e) {
            return Result.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return Result.fail(ErrorCode.FORBIDDEN, e.getMessage());
        } catch (PermissionDeniedException e) {
            return Result.fail(ErrorCode.FORBIDDEN, e.getMessage());
        } catch (InvalidTransitionException e) {
            return Result.fail(ErrorCode.INVALID_TRANSITION, e.getMessage());
        } catch (BusinessRuleException e) {
            return Result.fail(ErrorCode.BUSINESS_RULE, e.getMessage());
        } catch (RuntimeException e) {
            return Result.fail(ErrorCode.UNEXPECTED, "Unexpected error, please try again");
        }
    }

    // El userId activo que entrega el login simulado. Sin usuario no se puede operar.
    public static UserId activeUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("An active user is required");
        }
        return new UserId(userId);
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

    public static class InvalidInputException extends RuntimeException {

        public InvalidInputException(String message) {
            super(message);
        }
    }
}
