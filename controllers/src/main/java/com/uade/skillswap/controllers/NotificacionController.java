package com.uade.skillswap.controllers;

import com.uade.skillswap.application.notificaciones.PaginaNotificaciones;
import com.uade.skillswap.application.ports.in.BandejaNotificacionEntrada;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.notification.NotificationView;
import com.uade.skillswap.controllers.notification.NotificationController;
import com.uade.skillswap.domain.common.UserId;
import java.util.*;

public final class NotificacionController implements NotificationController {
    private final BandejaNotificacionEntrada bandeja;
    private final IdentityService identidad;

    /** La identidad es compartida con los demás controllers; la UI no elige el destinatario. */
    public NotificacionController(BandejaNotificacionEntrada bandeja, IdentityService identidad) {
        this.bandeja = Objects.requireNonNull(bandeja);
        this.identidad = Objects.requireNonNull(identidad);
    }

    @Override
    public ControllerResult<Page<NotificationView>> listar(int pagina, int tamanio) {
        return ControllerResult.of(() -> {
            String actor = actor();
            PaginaNotificaciones.validar(pagina, tamanio);
            var result = bandeja.listar(actor, pagina, tamanio);
            return new Page<>(result.elementos().stream().map(NotificationView::from).toList(),
                    new PageRequest(result.pagina(), result.tamanio()), result.total());
        });
    }

    @Override
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
