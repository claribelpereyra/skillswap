package com.uade.skillswap.application.dto.notification;

import com.uade.skillswap.domain.notificaciones.Notificacion;
import java.time.Instant;
import java.util.UUID;

/** Datos de la bandeja sin entidades JPA ni información de otros destinatarios. */
public record NotificationView(UUID id, String type, String reference, String message,
                               Instant occurredAt, String channel, String delivery,
                               boolean read, Instant readAt) {
    public static NotificationView from(Notificacion notification) {
        var notice = notification.aviso();
        return new NotificationView(notification.id(), notice.tipo().name(), notice.referencia(),
                notice.detalle(), notice.ocurridoEn(), notification.canal().name(),
                notification.entrega().name(), notification.leida(), notification.leidaEn());
    }
}
