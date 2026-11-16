package com.uade.skillswap.application.ports.in;

import com.uade.skillswap.domain.notificaciones.*;

public interface PreferenciaNotificacionEntrada {
    PreferenciaNotificacion consultar(String actorUserId);
    void cambiar(String actorUserId, CanalPreferido canal);
}
