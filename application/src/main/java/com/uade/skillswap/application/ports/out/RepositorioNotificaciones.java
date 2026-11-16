package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.application.notificaciones.PaginaNotificaciones;
import com.uade.skillswap.domain.notificaciones.*;
import java.time.Instant;
import java.util.UUID;

public interface RepositorioNotificaciones {
    /** Atómico: devuelve false si ya existe el par evento/destinatario. */
    boolean registrarSiAusente(Notificacion notificacion);
    /** Orden por ocurridoEn descendente y luego id descendente. */
    PaginaNotificaciones listar(String destinatario, int pagina, int tamanio);
    /** Atómico y limitado al dueño. true también si ya estaba leída, sin cambiar la fecha. */
    boolean marcarLeida(String destinatario, UUID id, Instant instante);
    void registrarEntrega(String destinatario, UUID id, EstadoEntrega estado);
}
