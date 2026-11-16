package com.uade.skillswap.infrastructure.configuracion;

import java.time.Duration;
import java.util.*;

/** Lee valores externos; no calcula reintegros ni ejecuta recomendaciones. */
public record ConfiguracionFuncional(Duration anticipacionReintegroTotal,
                                    Duration anticipacionReintegroParcial,
                                    EstrategiaRecomendacion estrategiaRecomendacion) {
    public enum EstrategiaRecomendacion { AFINIDAD, POPULARIDAD }

    public ConfiguracionFuncional {
        Objects.requireNonNull(anticipacionReintegroTotal);
        Objects.requireNonNull(anticipacionReintegroParcial);
        Objects.requireNonNull(estrategiaRecomendacion);
        if (anticipacionReintegroParcial.isNegative()
                || anticipacionReintegroTotal.compareTo(anticipacionReintegroParcial) <= 0) {
            throw new IllegalArgumentException("El umbral total debe ser mayor que el parcial, y ambos no negativos");
        }
    }

    public static ConfiguracionFuncional desdeEntorno() { return desde(System.getenv()); }

    public static ConfiguracionFuncional desde(Map<String, String> variables) {
        Objects.requireNonNull(variables);
        Duration total = horas(variables, "SKILLSWAP_CANCELACION_TOTAL_HORAS", "48");
        Duration parcial = horas(variables, "SKILLSWAP_CANCELACION_PARCIAL_HORAS", "24");
        String seleccion = valor(variables, "SKILLSWAP_RECOMENDACION", "AFINIDAD").toUpperCase(Locale.ROOT);
        EstrategiaRecomendacion estrategia;
        try {
            estrategia = EstrategiaRecomendacion.valueOf(seleccion);
        } catch (IllegalArgumentException fallo) {
            throw new IllegalArgumentException("SKILLSWAP_RECOMENDACION debe ser AFINIDAD o POPULARIDAD");
        }
        return new ConfiguracionFuncional(total, parcial, estrategia);
    }

    private static Duration horas(Map<String, String> variables, String nombre, String defecto) {
        try {
            return Duration.ofHours(Long.parseLong(valor(variables, nombre, defecto)));
        } catch (IllegalArgumentException | ArithmeticException fallo) {
            throw new IllegalArgumentException(nombre + " debe contener horas enteras dentro del rango permitido");
        }
    }

    private static String valor(Map<String, String> variables, String nombre, String defecto) {
        String valor = variables.get(nombre);
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }
}
