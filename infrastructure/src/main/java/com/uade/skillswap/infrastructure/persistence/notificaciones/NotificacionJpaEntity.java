package com.uade.skillswap.infrastructure.persistence.notificaciones;

import com.uade.skillswap.domain.notificaciones.*;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "notificaciones", uniqueConstraints =
    @UniqueConstraint(name = "uq_notificacion_evento_destinatario", columnNames = {"evento_id", "destinatario"}))
public class NotificacionJpaEntity {
    @Id @Column(length = 36) String id;
    @Column(name = "evento_id", nullable = false, length = 36) String eventoId;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name = "ocurrido_en", nullable = false) Instant ocurridoEn;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) TipoEventoNotificable tipo;
    @Column(nullable = false, length = 255) String referencia;
    @Column(nullable = false, length = 255) String destinatario;
    @Column(nullable = false, columnDefinition = "TEXT") String detalle;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) CanalPreferido canal;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) EstadoEntrega entrega;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name = "leida_en") Instant leidaEn;
    protected NotificacionJpaEntity() {}
}
