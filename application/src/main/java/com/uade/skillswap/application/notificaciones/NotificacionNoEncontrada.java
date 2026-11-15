package com.uade.skillswap.application.notificaciones;

/** No distingue entre un id ajeno y uno inexistente. */
public final class NotificacionNoEncontrada extends RuntimeException {
    public NotificacionNoEncontrada() { super("No se encontró la notificación del usuario"); }
}
