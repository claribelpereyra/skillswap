package com.uade.skillswap.dominio.calificacion.excepciones;

public class CalificacionInvalidaException extends RuntimeException {
    public CalificacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
