package com.uade.skillswap.infrastructure.persistence.notificaciones;

import com.uade.skillswap.domain.notificaciones.CanalPreferido;
import jakarta.persistence.*;

@Entity @Table(name = "preferencias_notificacion")
public class PreferenciaJpaEntity {
    @Id @Column(name = "user_id", length = 255) String userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) CanalPreferido canal;
    protected PreferenciaJpaEntity() {}
}
