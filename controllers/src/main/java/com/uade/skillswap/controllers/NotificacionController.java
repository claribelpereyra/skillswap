package com.uade.skillswap.controllers;

import com.uade.skillswap.application.notificaciones.PaginaNotificaciones;
import com.uade.skillswap.application.ports.in.BandejaNotificacionEntrada;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.domain.common.UserId;
import java.util.*;

public final class NotificacionController {
    private final BandejaNotificacionEntrada bandeja;
    private final IdentityService identidad;

    /** La identidad es compartida con los demás controllers; la UI no elige el destinatario. */
    public NotificacionController(BandejaNotificacionEntrada bandeja, IdentityService identidad) {
        this.bandeja = Objects.requireNonNull(bandeja);
        this.identidad = Objects.requireNonNull(identidad);
    }

    public ControllerResult<PaginaNotificaciones> listar(int pagina, int tamanio) {
        return ControllerResult.of(() -> {
            String actor = actor();
            PaginaNotificaciones.validar(pagina, tamanio);
            return bandeja.listar(actor, pagina, tamanio);
        });
    }

    public ControllerResult<Boolean> marcarLeida(UUID notificacionId) {
        return ControllerResult.of(() -> {
            String actor = actor();
            if (notificacionId == null) throw new IllegalArgumentException("Falta id de notificación");
            bandeja.marcarLeida(actor, notificacionId);
            return true;
        });
    }

    private String actor() {
        UserId actor = identidad.currentUserId();
        if (actor == null) throw new AccessDeniedException("No active user");
        return actor.value();
    }
}
