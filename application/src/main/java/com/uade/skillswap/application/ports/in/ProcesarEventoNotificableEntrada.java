package com.uade.skillswap.application.ports.in;

import com.uade.skillswap.domain.notificaciones.EventoNotificable;

@FunctionalInterface
public interface ProcesarEventoNotificableEntrada {
    void procesar(EventoNotificable evento);
}
