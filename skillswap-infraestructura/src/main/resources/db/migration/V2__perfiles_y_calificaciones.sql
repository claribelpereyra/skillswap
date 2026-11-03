-- Migración V2: Tablas de Perfiles, Habilidades y Calificaciones Verificadas (§4.1, §4.4, §8)
-- Responsable: Andy

CREATE TABLE IF NOT EXISTS perfiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    nombre_visible VARCHAR(150) NOT NULL,
    biografia TEXT,
    zona_horaria VARCHAR(50) NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    tipo_cuenta VARCHAR(30) NOT NULL,
    reputacion DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    cantidad_calificaciones INT NOT NULL DEFAULT 0,
    disponibilidad VARCHAR(255) DEFAULT 'No especificada',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_perfil_user_id UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS perfil_habilidades (
    perfil_id BIGINT NOT NULL,
    habilidad VARCHAR(100) NOT NULL,
    PRIMARY KEY (perfil_id, habilidad),
    CONSTRAINT fk_perfil_habilidades_perfil FOREIGN KEY (perfil_id) REFERENCES perfiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS perfil_intereses (
    perfil_id BIGINT NOT NULL,
    interes VARCHAR(100) NOT NULL,
    PRIMARY KEY (perfil_id, interes),
    CONSTRAINT fk_perfil_intereses_perfil FOREIGN KEY (perfil_id) REFERENCES perfiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS calificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contratacion_id BIGINT NOT NULL,
    autor_id VARCHAR(100) NOT NULL,
    destinatario_id VARCHAR(100) NOT NULL,
    puntaje INT NOT NULL,
    comentario TEXT,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_calificacion_puntaje CHECK (puntaje >= 1 AND puntaje <= 5),
    CONSTRAINT chk_calificacion_no_autocalificacion CHECK (autor_id <> destinatario_id),
    CONSTRAINT uq_calificacion_contratacion_autor UNIQUE (contratacion_id, autor_id, destinatario_id)
);

CREATE INDEX idx_perfil_user_id ON perfiles(user_id);
CREATE INDEX idx_perfil_reputacion ON perfiles(reputacion DESC);
CREATE INDEX idx_calificaciones_destinatario ON calificaciones(destinatario_id);
CREATE INDEX idx_calificaciones_contratacion ON calificaciones(contratacion_id);
