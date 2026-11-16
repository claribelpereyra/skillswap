package com.uade.skillswap.infraestructura.persistencia.notificaciones;

import com.uade.skillswap.dominio.notificaciones.CanalPreferido;
import jakarta.persistence.*;

@Entity @Table(name = "preferencias_notificacion")
public class PreferenciaJpaEntity {
    @Id @Column(name = "user_id", length = 255) String userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) CanalPreferido canal;
    protected PreferenciaJpaEntity() {}
}
