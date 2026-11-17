package com.uade.skillswap.application.ports.in;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.notification.NotificationView;
import java.util.UUID;

public interface BandejaNotificacionEntrada {
    Page<NotificationView> listar(String actorUserId, int pagina, int tamanio);
    void marcarLeida(String actorUserId, UUID notificacionId);
}
