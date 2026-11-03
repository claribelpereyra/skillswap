package com.uade.skillswap.dominio.perfil;

import java.util.Objects;

/**
 * Value Object que modela la disponibilidad horaria general declarada por el usuario.
 */
public record Disponibilidad(String descripcion) {

    public static final Disponibilidad NO_ESPECIFICADA = new Disponibilidad("No especificada");

    public Disponibilidad {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            descripcion = "No especificada";
        } else {
            descripcion = descripcion.trim();
        }
    }
}
