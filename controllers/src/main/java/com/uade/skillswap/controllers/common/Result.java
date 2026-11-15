package com.uade.skillswap.controllers.common;

import java.util.Objects;

// Resultado uniforme que devuelven los controllers a la UI: o un valor, o un error con código y mensaje.
// Así la UI nunca recibe una excepción y muestra todos los errores de la misma forma.
public record Result<T>(T value, Error error) {

    public record Error(ErrorCode code, String message) {

        public Error {
            Objects.requireNonNull(code, "The error code is required");
        }
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> fail(ErrorCode code, String message) {
        return new Result<>(null, new Error(code, message));
    }

    public boolean isOk() {
        return error == null;
    }
}
