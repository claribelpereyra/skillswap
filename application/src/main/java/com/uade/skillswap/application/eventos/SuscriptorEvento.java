package com.uade.skillswap.application.eventos;

import com.uade.skillswap.domain.eventos.EventoDominio;

public interface SuscriptorEvento {
    boolean acepta(EventoDominio evento);
    void recibir(EventoDominio evento);
}
