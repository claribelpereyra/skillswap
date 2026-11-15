package com.uade.skillswap.application.common;

// Otra operación guardó los mismos datos en el medio; reintentar suele alcanzar.
public class ConcurrentUpdateException extends ApplicationException {

    public ConcurrentUpdateException(Throwable cause) {
        super("The data was updated by someone else at the same time, please try again", cause);
    }
}
