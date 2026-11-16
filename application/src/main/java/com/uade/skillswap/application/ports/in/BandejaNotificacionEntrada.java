package com.uade.skillswap.application.ports.in;

import com.uade.skillswap.application.notificaciones.PaginaNotificaciones;
import java.util.UUID;

public interface BandejaNotificacionEntrada {
    PaginaNotificaciones listar(String actorUserId, int pagina, int tamanio);
    void marcarLeida(String actorUserId, UUID notificacionId);
}
