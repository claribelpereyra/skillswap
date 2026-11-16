CREATE TABLE preferencias_notificacion (
    user_id VARCHAR(255) NOT NULL PRIMARY KEY,
    canal VARCHAR(20) NOT NULL,
    CONSTRAINT chk_preferencia_canal CHECK (canal IN ('EMAIL', 'PUSH', 'INTERNA'))
);

-- user_id proviene de identidad externa; no requiere una tabla local de autenticación.
CREATE TABLE notificaciones (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    evento_id VARCHAR(36) NOT NULL,
    ocurrido_en DATETIME(6) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    referencia VARCHAR(255) NOT NULL,
    destinatario VARCHAR(255) NOT NULL,
    detalle TEXT NOT NULL,
    canal VARCHAR(20) NOT NULL,
    entrega VARCHAR(20) NOT NULL,
    leida_en DATETIME(6) NULL,
    CONSTRAINT uq_notificacion_evento_destinatario UNIQUE (evento_id, destinatario),
    CONSTRAINT chk_notificacion_canal CHECK (canal IN ('EMAIL', 'PUSH', 'INTERNA')),
    CONSTRAINT chk_notificacion_entrega CHECK (entrega IN ('PENDIENTE', 'ENVIADA', 'FALLIDA')),
    CONSTRAINT chk_notificacion_tipo CHECK (tipo IN ('SOLICITUD_RECIBIDA', 'ACEPTACION',
        'PAGO_CONFIRMADO', 'REPROGRAMACION', 'CANCELACION', 'SESION_COMPLETADA'))
);

CREATE INDEX idx_notificaciones_bandeja ON notificaciones (destinatario, ocurrido_en, id);
