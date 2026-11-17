package com.uade.skillswap.infrastructure.configuracion;

import java.time.Duration;
import java.util.*;

/** Lee valores externos; no calcula reintegros, no ejecuta recomendaciones ni aplica promociones. */
public record ConfiguracionFuncional(Duration anticipacionReintegroTotal,
                                    Duration anticipacionReintegroParcial,
                                    EstrategiaRecomendacion estrategiaRecomendacion,
                                    Map<String, List<PromocionConfigurada>> promocionesPorProveedor,
                                    int candidatosRecomendacion) {
    public enum EstrategiaRecomendacion { AFINIDAD, POPULARIDAD }

    public enum TipoPromocion { PORCENTAJE, FIJO }

    public record PromocionConfigurada(TipoPromocion tipo, long valor) {
        public PromocionConfigurada {
            Objects.requireNonNull(tipo);
            if (valor < 0 || (tipo == TipoPromocion.PORCENTAJE && valor > 100)) {
                throw new IllegalArgumentException(
                        "SKILLSWAP_PROMOCIONES: PORCENTAJE va de 0 a 100 y FIJO no puede ser negativo");
            }
        }
    }

    public ConfiguracionFuncional {
        Objects.requireNonNull(anticipacionReintegroTotal);
        Objects.requireNonNull(anticipacionReintegroParcial);
        Objects.requireNonNull(estrategiaRecomendacion);
        if (anticipacionReintegroParcial.isNegative()
                || anticipacionReintegroTotal.compareTo(anticipacionReintegroParcial) <= 0) {
            throw new IllegalArgumentException("El umbral total debe ser mayor que el parcial, y ambos no negativos");
        }
        Map<String, List<PromocionConfigurada>> copia = new HashMap<>();
        Objects.requireNonNull(promocionesPorProveedor).forEach((proveedor, promociones) ->
                copia.put(proveedor, List.copyOf(promociones)));
        promocionesPorProveedor = Map.copyOf(copia);
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
        return new ConfiguracionFuncional(total, parcial, estrategia, promociones(variables),
                candidatos(variables));
    }

    // Formato: proveedor=TIPO:valor,TIPO:valor;otroProveedor=TIPO:valor. Las promociones se aplican en ese orden.
    private static Map<String, List<PromocionConfigurada>> promociones(Map<String, String> variables) {
        String texto = valor(variables, "SKILLSWAP_PROMOCIONES", "");
        Map<String, List<PromocionConfigurada>> porProveedor = new HashMap<>();
        if (texto.isEmpty()) {
            return porProveedor;
        }
        for (String grupo : texto.split(";", -1)) {
            String[] partes = grupo.split("=", -1);
            if (partes.length != 2 || partes[0].isBlank() || partes[1].isBlank()) {
                throw formatoDePromocionInvalido();
            }
            List<PromocionConfigurada> promociones = new ArrayList<>();
            for (String promocion : partes[1].split(",", -1)) {
                promociones.add(promocion(promocion));
            }
            if (porProveedor.putIfAbsent(partes[0].trim(), promociones) != null) {
                throw new IllegalArgumentException("SKILLSWAP_PROMOCIONES repite el proveedor " + partes[0].trim());
            }
        }
        return porProveedor;
    }

    private static PromocionConfigurada promocion(String texto) {
        String[] partes = texto.split(":", -1);
        if (partes.length != 2) {
            throw formatoDePromocionInvalido();
        }
        TipoPromocion tipo;
        long valor;
        try {
            tipo = TipoPromocion.valueOf(partes[0].trim().toUpperCase(Locale.ROOT));
            valor = Long.parseLong(partes[1].trim());
        } catch (IllegalArgumentException fallo) {
            throw formatoDePromocionInvalido();
        }
        return new PromocionConfigurada(tipo, valor);
    }

    private static IllegalArgumentException formatoDePromocionInvalido() {
        return new IllegalArgumentException(
                "SKILLSWAP_PROMOCIONES debe tener el formato proveedor=PORCENTAJE:10,FIJO:5;otroProveedor=FIJO:20");
    }

    private static int candidatos(Map<String, String> variables) {
        int candidatos;
        try {
            candidatos = Integer.parseInt(valor(variables, "SKILLSWAP_RECOMENDACION_CANDIDATOS", "200"));
        } catch (NumberFormatException fallo) {
            candidatos = 0;
        }
        if (candidatos < 1) {
            throw new IllegalArgumentException("SKILLSWAP_RECOMENDACION_CANDIDATOS debe ser un entero positivo");
        }
        return candidatos;
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
