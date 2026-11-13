CREATE TABLE IF NOT EXISTS info_esquema (
    clave VARCHAR(50)  NOT NULL PRIMARY KEY,
    valor VARCHAR(255) NOT NULL
);

INSERT INTO info_esquema (clave, valor) VALUES ('proyecto', 'SkillSwap');
