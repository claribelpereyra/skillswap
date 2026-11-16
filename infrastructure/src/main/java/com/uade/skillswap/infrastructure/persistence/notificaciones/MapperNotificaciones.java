package com.uade.skillswap.infrastructure.persistence.notificaciones;

import com.uade.skillswap.domain.notificaciones.*;
import java.util.UUID;

final class MapperNotificaciones {
    private MapperNotificaciones() {}

    static PreferenciaJpaEntity aJpa(PreferenciaNotificacion preferencia) {
        var entidad = new PreferenciaJpaEntity();
        entidad.userId = preferencia.userId();
        entidad.canal = preferencia.canal();
        return entidad;
    }

    static PreferenciaNotificacion aDominio(PreferenciaJpaEntity entidad) {
        return new PreferenciaNotificacion(entidad.userId, entidad.canal);
    }

    static NotificacionJpaEntity aJpa(Notificacion notificacion) {
        var entidad = new NotificacionJpaEntity();
        var aviso = notificacion.aviso();
        entidad.id = notificacion.id().toString();
        entidad.eventoId = aviso.eventoId().toString();
        entidad.ocurridoEn = aviso.ocurridoEn();
        entidad.tipo = aviso.tipo();
        entidad.referencia = aviso.referencia();
        entidad.destinatario = aviso.destinatario();
        entidad.detalle = aviso.detalle();
        entidad.canal = notificacion.canal();
        entidad.entrega = notificacion.entrega();
        entidad.leidaEn = notificacion.leidaEn();
        return entidad;
    }

    static Notificacion aDominio(NotificacionJpaEntity entidad) {
        var aviso = new Aviso(UUID.fromString(entidad.eventoId), entidad.ocurridoEn, entidad.tipo,
                entidad.referencia, entidad.destinatario, entidad.detalle);
        return new Notificacion(UUID.fromString(entidad.id), aviso, entidad.canal, entidad.entrega, entidad.leidaEn);
    }
}
