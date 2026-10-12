-- Migración inicial de SkillSwap.
-- Reglas del equipo:
--   * Nunca editar una migración que ya se mergeó: los cambios van en una nueva (V2__..., V3__...).
--   * Nombre: V<numero>__<descripcion_en_snake_case>.sql
--   * Los datos de demo reproducibles van en su propia migración (por ejemplo V100__datos_demo.sql).

CREATE TABLE IF NOT EXISTS info_esquema (
    clave VARCHAR(50)  NOT NULL PRIMARY KEY,
    valor VARCHAR(255) NOT NULL
);

INSERT INTO info_esquema (clave, valor) VALUES ('proyecto', 'SkillSwap');
