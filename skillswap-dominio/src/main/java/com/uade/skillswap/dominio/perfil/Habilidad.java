package com.uade.skillswap.dominio.perfil;

import com.uade.skillswap.dominio.perfil.excepciones.PerfilInvalidoException;

import java.util.Objects;

/**
 * Value Object inmutable que representa una competencia técnica o profesional.
 */
public record Habilidad(String nombre) {

    public Habilidad {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new PerfilInvalidoException("El nombre de la habilidad no puede ser nulo ni vacío.");
        }
        nombre = nombre.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Habilidad habilidad = (Habilidad) o;
        return nombre.equalsIgnoreCase(habilidad.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre.toLowerCase());
    }
}
