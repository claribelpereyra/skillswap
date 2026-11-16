package com.uade.skillswap.app.presentation;

/** Error de aplicación apto para mostrar en la pantalla. */
public final class UiOperationException extends RuntimeException {
    public UiOperationException(String message) { super(message); }
}
