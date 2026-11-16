package com.uade.skillswap.aplicacion.puertos.entrada;

import com.uade.skillswap.dominio.notificaciones.*;

public interface PreferenciaNotificacionEntrada {
    PreferenciaNotificacion consultar(String actorUserId);
    void cambiar(String actorUserId, CanalPreferido canal);
}
