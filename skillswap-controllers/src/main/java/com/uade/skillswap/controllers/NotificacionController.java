package com.uade.skillswap.controllers;

import com.uade.skillswap.aplicacion.notificaciones.PaginaNotificaciones;
import com.uade.skillswap.aplicacion.puertos.entrada.BandejaNotificacionEntrada;
import com.uade.skillswap.controllers.comun.*;
import java.util.*;
import java.util.function.Supplier;

public final class NotificacionController {
    private final BandejaNotificacionEntrada bandeja;
    private final Supplier<String> usuarioActivo;

    /** El supplier debe conectarse a identidad mock, nunca a un campo editable de la UI. */
    public NotificacionController(BandejaNotificacionEntrada bandeja, Supplier<String> usuarioActivo) {
        this.bandeja = Objects.requireNonNull(bandeja);
        this.usuarioActivo = Objects.requireNonNull(usuarioActivo);
    }

    public Resultado<PaginaNotificaciones> listar(int pagina, int tamanio) {
        return EjecutorController.ejecutar(() -> {
            String actor = actor();
            PaginaNotificaciones.validar(pagina, tamanio);
            return bandeja.listar(actor, pagina, tamanio);
        });
    }

    public Resultado<Boolean> marcarLeida(UUID notificacionId) {
        return EjecutorController.ejecutar(() -> {
            String actor = actor();
            if (notificacionId == null) throw new IllegalArgumentException("Falta id de notificación");
            bandeja.marcarLeida(actor, notificacionId);
            return true;
        });
    }

    private String actor() {
        String actor = usuarioActivo.get();
        if (actor == null || actor.isBlank()) throw new AccesoNoAutorizado();
        return actor;
    }
}
