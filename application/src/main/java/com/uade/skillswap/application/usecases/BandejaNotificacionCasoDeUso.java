package com.uade.skillswap.application.usecases;

import com.uade.skillswap.application.notificaciones.*;
import com.uade.skillswap.application.ports.in.BandejaNotificacionEntrada;
import com.uade.skillswap.application.ports.out.RepositorioNotificaciones;
import java.time.Clock;
import java.util.*;

public final class BandejaNotificacionCasoDeUso implements BandejaNotificacionEntrada {
    private final RepositorioNotificaciones repositorio;
    private final Clock reloj;

    public BandejaNotificacionCasoDeUso(RepositorioNotificaciones repositorio, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override public PaginaNotificaciones listar(String actorUserId, int pagina, int tamanio) {
        validarActor(actorUserId);
        PaginaNotificaciones.validar(pagina, tamanio);
        return repositorio.listar(actorUserId, pagina, tamanio);
    }

    @Override public void marcarLeida(String actorUserId, UUID notificacionId) {
        validarActor(actorUserId);
        Objects.requireNonNull(notificacionId);
        if (!repositorio.marcarLeida(actorUserId, notificacionId, reloj.instant())) {
            throw new NotificacionNoEncontrada();
        }
    }

    private static void validarActor(String actor) {
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("Falta usuario activo");
    }
}
