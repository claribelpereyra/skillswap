package com.uade.skillswap.aplicacion.eventos;

import com.uade.skillswap.aplicacion.puertos.salida.PublicadorEventos;
import com.uade.skillswap.dominio.eventos.EventoDominio;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Observer síncrono local. No es una cola durable ni administra transacciones. */
public final class PublicadorEventosEnMemoria implements PublicadorEventos {
    private final CopyOnWriteArrayList<SuscriptorEvento> suscriptores = new CopyOnWriteArrayList<>();

    public void suscribir(SuscriptorEvento suscriptor) {
        suscriptores.addIfAbsent(Objects.requireNonNull(suscriptor));
    }

    public void desuscribir(SuscriptorEvento suscriptor) {
        suscriptores.remove(Objects.requireNonNull(suscriptor));
    }

    @Override
    public void publicar(EventoDominio evento) {
        Objects.requireNonNull(evento);
        IllegalStateException fallos = new IllegalStateException("Fallaron suscriptores del evento " + evento.id());
        for (SuscriptorEvento suscriptor : suscriptores) {
            try {
                if (suscriptor.acepta(evento)) suscriptor.recibir(evento);
            } catch (RuntimeException fallo) {
                fallos.addSuppressed(fallo);
            }
        }
        if (fallos.getSuppressed().length > 0) throw fallos;
    }
}
