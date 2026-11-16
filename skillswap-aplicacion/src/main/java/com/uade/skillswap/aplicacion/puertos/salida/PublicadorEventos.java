package com.uade.skillswap.aplicacion.puertos.salida;

import com.uade.skillswap.dominio.eventos.EventoDominio;

public interface PublicadorEventos {
    /** Invocar tras el commit, nunca para anunciar una operación revertida. */
    void publicar(EventoDominio evento);
}
