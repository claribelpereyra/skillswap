package com.uade.skillswap.dominio.perfil.excepciones;

public class PerfilInactivoException extends RuntimeException {
    public PerfilInactivoException(String mensaje) {
        super(mensaje);
    }
}
