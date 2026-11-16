package com.uade.skillswap.application.usecases;

import com.uade.skillswap.application.ports.in.PreferenciaNotificacionEntrada;
import com.uade.skillswap.application.ports.out.RepositorioPreferencias;
import com.uade.skillswap.domain.notificaciones.*;
import java.util.Objects;

public final class PreferenciaNotificacionCasoDeUso implements PreferenciaNotificacionEntrada {
    private final RepositorioPreferencias repositorio;

    public PreferenciaNotificacionCasoDeUso(RepositorioPreferencias repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    @Override public PreferenciaNotificacion consultar(String actorUserId) {
        var inicial = new PreferenciaNotificacion(actorUserId, CanalPreferido.INTERNA);
        return repositorio.buscar(actorUserId).orElse(inicial);
    }

    @Override public void cambiar(String actorUserId, CanalPreferido canal) {
        repositorio.guardar(new PreferenciaNotificacion(actorUserId, canal));
    }
}
