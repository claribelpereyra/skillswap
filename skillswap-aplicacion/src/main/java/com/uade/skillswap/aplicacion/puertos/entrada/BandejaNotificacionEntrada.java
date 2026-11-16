package com.uade.skillswap.aplicacion.puertos.entrada;

import com.uade.skillswap.aplicacion.notificaciones.PaginaNotificaciones;
import java.util.UUID;

public interface BandejaNotificacionEntrada {
    PaginaNotificaciones listar(String actorUserId, int pagina, int tamanio);
    void marcarLeida(String actorUserId, UUID notificacionId);
}
