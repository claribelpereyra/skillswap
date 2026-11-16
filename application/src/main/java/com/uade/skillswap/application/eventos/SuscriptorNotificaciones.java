package com.uade.skillswap.application.eventos;

import com.uade.skillswap.application.ports.in.ProcesarEventoNotificableEntrada;
import com.uade.skillswap.domain.eventos.EventoDominio;
import com.uade.skillswap.domain.notificaciones.EventoNotificable;
import java.util.Objects;

/** Un único suscriptor atiende los seis tipos sin duplicar el mecanismo Observer. */
public final class SuscriptorNotificaciones implements SuscriptorEvento {
    private final ProcesarEventoNotificableEntrada procesador;

    public SuscriptorNotificaciones(ProcesarEventoNotificableEntrada procesador) {
        this.procesador = Objects.requireNonNull(procesador);
    }

    @Override public boolean acepta(EventoDominio evento) {
        return evento instanceof EventoNotificable;
    }

    @Override public void recibir(EventoDominio evento) {
        if (evento instanceof EventoNotificable notificable) procesador.procesar(notificable);
    }
}
