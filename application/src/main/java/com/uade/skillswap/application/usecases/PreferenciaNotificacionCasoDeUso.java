package com.uade.skillswap.application.usecases;

import com.uade.skillswap.application.dto.notification.NotificationPreferenceView;
import com.uade.skillswap.application.ports.in.PreferenciaNotificacionEntrada;
import com.uade.skillswap.application.ports.out.RepositorioPreferencias;
import com.uade.skillswap.domain.notificaciones.*;
import java.util.Objects;

public final class PreferenciaNotificacionCasoDeUso implements PreferenciaNotificacionEntrada {
    private final RepositorioPreferencias repositorio;

    public PreferenciaNotificacionCasoDeUso(RepositorioPreferencias repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    @Override public NotificationPreferenceView consultar(String actorUserId) {
        var inicial = new PreferenciaNotificacion(actorUserId, CanalPreferido.INTERNA);
        return NotificationPreferenceView.from(repositorio.buscar(actorUserId).orElse(inicial));
    }

    @Override public void cambiar(String actorUserId, CanalPreferido canal) {
        repositorio.guardar(new PreferenciaNotificacion(actorUserId, canal));
    }
}
