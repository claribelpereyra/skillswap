package com.uade.skillswap.application.notificaciones;

/** No distingue entre un id ajeno y uno inexistente. */
public final class NotificacionNoEncontrada extends com.uade.skillswap.application.common.ResourceNotFoundException {
    public NotificacionNoEncontrada() { super("Notification for current user", "requested id"); }
}
