package com.uade.skillswap.infrastructure.adapters.notificaciones;

import com.uade.skillswap.application.ports.out.CanalNotificacion;
import com.uade.skillswap.domain.notificaciones.Aviso;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registro observable para demos y pruebas; no contacta proveedores reales. */
public abstract class CanalMock implements CanalNotificacion {
    private final List<Aviso> enviados = new CopyOnWriteArrayList<>();

    @Override public final void enviar(Aviso aviso) { enviados.add(Objects.requireNonNull(aviso)); }
    public final List<Aviso> enviados() { return List.copyOf(enviados); }
}
