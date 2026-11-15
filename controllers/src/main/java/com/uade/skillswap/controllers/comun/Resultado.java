package com.uade.skillswap.controllers.comun;

import java.util.Objects;

/** Un resultado contiene datos o error, nunca los dos ni ninguno. */
public record Resultado<T>(T datos, ErrorController error) {
    public Resultado {
        if ((datos == null) == (error == null)) throw new IllegalArgumentException("Resultado ambiguo");
    }

    public boolean exitoso() { return error == null; }
    public static <T> Resultado<T> exito(T datos) { return new Resultado<>(Objects.requireNonNull(datos), null); }
    public static <T> Resultado<T> fallo(CodigoError codigo, String mensaje) {
        return new Resultado<>(null, new ErrorController(codigo, mensaje));
    }
}
