package com.uade.skillswap.dominio.perfil.excepciones;

public class PerfilInvalidoException extends RuntimeException {
    public PerfilInvalidoException(String mensaje) {
        super(mensaje);
    }
}
