package com.uade.skillswap.application.ports.in;

import com.uade.skillswap.application.dto.notification.NotificationPreferenceView;
import com.uade.skillswap.domain.notificaciones.CanalPreferido;

public interface PreferenciaNotificacionEntrada {
    NotificationPreferenceView consultar(String actorUserId);
    void cambiar(String actorUserId, CanalPreferido canal);
}
