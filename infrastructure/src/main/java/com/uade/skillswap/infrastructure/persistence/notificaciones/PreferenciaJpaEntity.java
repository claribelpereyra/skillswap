package com.uade.skillswap.infrastructure.persistence.notificaciones;

import com.uade.skillswap.domain.notificaciones.CanalPreferido;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "preferencias_notificacion")
public class PreferenciaJpaEntity {
    @Id @Column(name = "user_id", length = 255) String userId;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) CanalPreferido canal;
    protected PreferenciaJpaEntity() {}
}
