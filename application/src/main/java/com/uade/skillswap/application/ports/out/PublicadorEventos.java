package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.domain.eventos.EventoDominio;

public interface PublicadorEventos {
    /** Invocar tras el commit, nunca para anunciar una operación revertida. */
    void publicar(EventoDominio evento);
}
