package com.uade.skillswap.controllers.comun;

import java.util.Objects;

public record ErrorController(CodigoError codigo, String mensaje) {
    public ErrorController {
        Objects.requireNonNull(codigo);
        if (mensaje == null || mensaje.isBlank()) throw new IllegalArgumentException("Falta mensaje de error");
    }
}
