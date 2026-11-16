package com.uade.skillswap.aplicacion.eventos;

import com.uade.skillswap.dominio.eventos.EventoDominio;

public interface SuscriptorEvento {
    boolean acepta(EventoDominio evento);
    void recibir(EventoDominio evento);
}
