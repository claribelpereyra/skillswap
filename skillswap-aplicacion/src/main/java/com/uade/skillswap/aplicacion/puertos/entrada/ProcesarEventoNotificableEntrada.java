package com.uade.skillswap.aplicacion.puertos.entrada;

import com.uade.skillswap.dominio.notificaciones.EventoNotificable;

@FunctionalInterface
public interface ProcesarEventoNotificableEntrada {
    void procesar(EventoNotificable evento);
}
