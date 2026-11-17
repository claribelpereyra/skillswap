package com.uade.skillswap.application.dto.notification;

import com.uade.skillswap.domain.notificaciones.CanalPreferido;
import com.uade.skillswap.domain.notificaciones.PreferenciaNotificacion;

/** Canal elegido por el usuario, sin exponer la entidad de dominio fuera de la aplicación. */
public record NotificationPreferenceView(String userId, CanalPreferido channel) {
    public static NotificationPreferenceView from(PreferenciaNotificacion preference) {
        return new NotificationPreferenceView(preference.userId(), preference.canal());
    }
}
