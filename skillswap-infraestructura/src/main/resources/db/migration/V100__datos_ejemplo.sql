-- Migración V100: Datos de ejemplo reproducibles para entorno local (§8)
-- Responsable: Andy

-- 1. Perfiles de prueba
INSERT INTO perfiles (id, user_id, nombre_visible, biografia, zona_horaria, tipo_cuenta, reputacion, cantidad_calificaciones, disponibilidad, activo)
VALUES 
    (1, 'usr-andy', 'Andy Backend Lead', 'Desarrollador Java especializado en Arquitectura Limpia y patrones GoF.', 'America/Argentina/Buenos_Aires', 'PROFESIONAL', 5.00, 2, 'Lunes a Viernes 18:00 a 21:00', TRUE),
    (2, 'usr-clari', 'Clari Arquitecta', 'Especialista en flujos de concurrencia y máquinas de estado en Java.', 'America/Argentina/Buenos_Aires', 'PROFESIONAL', 4.80, 5, 'Lunes a Jueves 17:00 a 20:00', TRUE),
    (3, 'usr-luqui', 'Lucas Frontend & Catálogo', 'Experto en estructuras jerárquicas y optimización de búsquedas.', 'America/Argentina/Buenos_Aires', 'PROFESIONAL', 4.50, 4, 'Sábados de 10:00 a 14:00', TRUE),
    (4, 'usr-tomi', 'Tomás FinTech Dev', 'Desarrollador con foco en transacciones de crédito y billeteras virtuales.', 'America/Argentina/Buenos_Aires', 'PROFESIONAL', 4.75, 4, 'Lunes a Viernes 19:00 a 22:00', TRUE),
    (5, 'usr-ian', 'Ian Integrador', 'Diseño de eventos de dominio, notificaciones e interfaces Swing/JavaFX.', 'America/Argentina/Buenos_Aires', 'PROFESIONAL', 5.00, 1, 'Horarios flexibles', TRUE),
    (6, 'org-uade-tech', 'UADE Tech Hub', 'Comunidad tecnológica universitaria orientada a desafíos de software.', 'America/Argentina/Buenos_Aires', 'ORGANIZACION', 4.90, 10, 'Lunes a Viernes 09:00 a 18:00', TRUE),
    (7, 'usr-cliente-demo', 'Martín Alumno', 'Estudiante de ingeniería buscando mentorías en diseño de software.', 'America/Argentina/Buenos_Aires', 'PROFESIONAL', 0.00, 0, 'No especificada', TRUE);

-- 2. Habilidades de los perfiles
INSERT INTO perfil_habilidades (perfil_id, habilidad)
VALUES
    (1, 'Java'), (1, 'Clean Architecture'), (1, 'JUnit 5'), (1, 'Hibernate'),
    (2, 'Concurrencia'), (2, 'State Pattern'), (2, 'Optimistic Locking'),
    (3, 'Composite Pattern'), (3, 'Búsqueda Paginada'), (3, 'StarUML'),
    (4, 'Strategy Pattern'), (4, 'Decorator'), (4, 'FinTech'),
    (5, 'Observer Pattern'), (5, 'Event-Driven'), (5, 'JavaFX'),
    (6, 'Innovación'), (6, 'Desafíos'), (6, 'Hackathon');

-- 3. Intereses de los perfiles
INSERT INTO perfil_intereses (perfil_id, interes)
VALUES
    (1, 'Patrones de Diseño'), (1, 'Bases de Datos Relacionales'),
    (7, 'Aprender Java'), (7, 'Arquitectura de Software'), (7, 'Patrones GoF');

-- 4. Calificaciones verificadas de prueba
INSERT INTO calificaciones (id, contratacion_id, autor_id, destinatario_id, puntaje, comentario)
VALUES
    (1, 101, 'usr-cliente-demo', 'usr-andy', 5, 'Excelente explicación de los principios SOLID y Clean Architecture.'),
    (2, 102, 'usr-clari', 'usr-andy', 5, 'Muy claro y profesional en la sesión de mentoría.'),
    (3, 103, 'usr-cliente-demo', 'usr-clari', 5, 'Gran manejo del patrón State para la máquina de estados.');
