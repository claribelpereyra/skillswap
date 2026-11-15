package com.uade.skillswap.application.common;

// Base de los errores propios de la capa de aplicación (permisos, recursos inexistentes).
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }

    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
