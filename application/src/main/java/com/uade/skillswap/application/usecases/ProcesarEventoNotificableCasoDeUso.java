package com.uade.skillswap.application.usecases;

import com.uade.skillswap.application.notificaciones.FabricaCanalNotificacion;
import com.uade.skillswap.application.ports.in.*;
import com.uade.skillswap.application.ports.out.RepositorioNotificaciones;
import com.uade.skillswap.domain.notificaciones.*;
import java.util.*;

/** Se ejecuta después de la transacción del negocio. No reenvía eventos ya registrados. */
public final class ProcesarEventoNotificableCasoDeUso implements ProcesarEventoNotificableEntrada {
    private final PreferenciaNotificacionEntrada preferencias;
    private final RepositorioNotificaciones repositorio;
    private final FabricaCanalNotificacion fabrica;

    public ProcesarEventoNotificableCasoDeUso(PreferenciaNotificacionEntrada preferencias,
            RepositorioNotificaciones repositorio, FabricaCanalNotificacion fabrica) {
        this.preferencias = Objects.requireNonNull(preferencias);
        this.repositorio = Objects.requireNonNull(repositorio);
        this.fabrica = Objects.requireNonNull(fabrica);
    }

    @Override public void procesar(EventoNotificable evento) {
        Objects.requireNonNull(evento);
        var fallos = new IllegalStateException("No se pudieron procesar todos los avisos de " + evento.id());
        for (String usuario : evento.destinatarios()) {
            try {
                procesarDestinatario(evento, usuario);
            } catch (RuntimeException fallo) {
                fallos.addSuppressed(fallo);
            }
        }
        if (fallos.getSuppressed().length > 0) throw fallos;
    }

    private void procesarDestinatario(EventoNotificable evento, String usuario) {
        var canal = preferencias.consultar(usuario).canal();
        var aviso = new Aviso(evento.id(), evento.ocurridoEn(), evento.tipo(), evento.referencia(), usuario, evento.detalle());
        var notificacion = new Notificacion(UUID.randomUUID(), aviso, canal, EstadoEntrega.PENDIENTE, null);
        if (!repositorio.registrarSiAusente(notificacion)) return;
        try {
            fabrica.crear(canal).enviar(aviso);
        } catch (RuntimeException fallo) {
            try {
                repositorio.registrarEntrega(usuario, notificacion.id(), EstadoEntrega.FALLIDA);
            } catch (RuntimeException falloRegistro) {
                fallo.addSuppressed(falloRegistro);
            }
            throw fallo;
        }
        // Si falla guardar el resultado, queda PENDIENTE: no se afirma que el envío falló.
        repositorio.registrarEntrega(usuario, notificacion.id(), EstadoEntrega.ENVIADA);
    }
}
