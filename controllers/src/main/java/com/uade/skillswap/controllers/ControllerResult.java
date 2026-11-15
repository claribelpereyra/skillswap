package com.uade.skillswap.controllers;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.PermissionDeniedException;

import java.util.function.Supplier;

// Manejo uniforme de errores: la interfaz nunca recibe una excepción, recibe el dato o el error.
public final class ControllerResult<T> {

    public enum ErrorType {
        BUSINESS_RULE,
        ACCESS_DENIED,
        NOT_FOUND,
        CONFLICT,
        INVALID_INPUT,
        UNEXPECTED
    }

    private final T data;
    private final ErrorType errorType;
    private final String message;

    private ControllerResult(T data, ErrorType errorType, String message) {
        this.data = data;
        this.errorType = errorType;
        this.message = message;
    }

    public static <T> ControllerResult<T> success(T data) {
        return new ControllerResult<>(data, null, null);
    }

    public static <T> ControllerResult<T> failure(ErrorType errorType, String message) {
        return new ControllerResult<>(null, errorType, message);
    }

    public static <T> ControllerResult<T> of(Supplier<T> operation) {
        try {
            return success(operation.get());
        } catch (PermissionDeniedException error) {
            return failure(ErrorType.ACCESS_DENIED, error.getMessage());
        } catch (BusinessRuleException error) {
            return failure(ErrorType.BUSINESS_RULE, error.getMessage());
        } catch (AccessDeniedException error) {
            return failure(ErrorType.ACCESS_DENIED, error.getMessage());
        } catch (ResourceNotFoundException error) {
            return failure(ErrorType.NOT_FOUND, error.getMessage());
        } catch (ConcurrentUpdateException error) {
            return failure(ErrorType.CONFLICT, error.getMessage());
        } catch (IllegalArgumentException error) {
            return failure(ErrorType.INVALID_INPUT, error.getMessage());
        } catch (RuntimeException error) {
            // No se muestra el detalle técnico: puede revelar información interna.
            return failure(ErrorType.UNEXPECTED, "An unexpected error occurred");
        }
    }

    public boolean isSuccess() {
        return errorType == null;
    }

    public T getData() {
        return data;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }
}
