package com.uade.skillswap.aplicacion.eventos;

import com.uade.skillswap.aplicacion.puertos.entrada.ProcesarEventoNotificableEntrada;
import com.uade.skillswap.dominio.eventos.EventoDominio;
import com.uade.skillswap.dominio.notificaciones.EventoNotificable;
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
