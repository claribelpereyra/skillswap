# Revisión de Claves, Unicidad e Índices del Esquema Relacional

**Materia:** Procesos de Desarrollo de Software (PDS - UADE)  
**Responsable:** Andy (§8)  
**Objetivo:** Definir el diseño relacional del modelo de datos para Perfiles, Habilidades y Calificaciones, garantizando integridad referencial, restricciones de unicidad y optimización de consultas sin problemas de $N+1$.

---

## 1. Tablas del Módulo de Perfiles y Calificaciones

### 1.1. Tabla `perfiles`
Almacena los agregados principales de usuarios de la plataforma (Profesionales y Organizaciones).

```sql
CREATE TABLE perfiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    nombre_visible VARCHAR(150) NOT NULL,
    biografia TEXT,
    zona_horaria VARCHAR(50) NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    tipo_cuenta VARCHAR(30) NOT NULL, -- 'PROFESIONAL' o 'ORGANIZACION'
    reputacion DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    cantidad_calificaciones INT NOT NULL DEFAULT 0,
    disponibilidad VARCHAR(255) DEFAULT 'No especificada',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_perfil_user_id UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índice para búsquedas rápidas por identidad y ordenamiento por reputación sin full-table scan
CREATE INDEX idx_perfil_user_id ON perfiles(user_id);
CREATE INDEX idx_perfil_reputacion ON perfiles(reputacion DESC);
```

### 1.2. Tabla `perfil_habilidades`
Mapea la colección de habilidades asociadas al perfil (Value Object `Habilidad`).

```sql
CREATE TABLE perfil_habilidades (
    perfil_id BIGINT NOT NULL,
    habilidad VARCHAR(100) NOT NULL,
    
    PRIMARY KEY (perfil_id, habilidad),
    CONSTRAINT fk_perfil_habilidades_perfil FOREIGN KEY (perfil_id) 
        REFERENCES perfiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índice para permitir cruzar habilidades con las publicaciones en el algoritmo de recomendación
CREATE INDEX idx_perfil_habilidad ON perfil_habilidades(habilidad);
```

### 1.3. Tabla `perfil_intereses`
Mapea los intereses declarados por el usuario para alimentar el motor de recomendación por afinidad.

```sql
CREATE TABLE perfil_intereses (
    perfil_id BIGINT NOT NULL,
    interes VARCHAR(100) NOT NULL,
    
    PRIMARY KEY (perfil_id, interes),
    CONSTRAINT fk_perfil_intereses_perfil FOREIGN KEY (perfil_id) 
        REFERENCES perfiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 1.4. Tabla `calificaciones`
Almacena las calificaciones verificadas emitidas entre participantes de una contratación completada.

```sql
CREATE TABLE calificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contratacion_id BIGINT NOT NULL,
    autor_id VARCHAR(100) NOT NULL,
    destinatario_id VARCHAR(100) NOT NULL,
    puntaje INT NOT NULL,
    comentario TEXT,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Restricción de Invariante: puntaje obligatorio entre 1 y 5
    CONSTRAINT chk_calificacion_puntaje CHECK (puntaje >= 1 AND puntaje <= 5),
    
    -- Restricción de Invariante: no se permite autocalificación
    CONSTRAINT chk_calificacion_no_autocalificacion CHECK (autor_id <> destinatario_id),
    
    -- Restricción de Unicidad Estricta (§4.4, §8):
    -- Un participante solo puede calificar una única vez por contratación y contraparte
    CONSTRAINT uq_calificacion_contratacion_autor UNIQUE (contratacion_id, autor_id, destinatario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices de consulta frecuente
CREATE INDEX idx_calificaciones_destinatario ON calificaciones(destinatario_id);
CREATE INDEX idx_calificaciones_contratacion ON calificaciones(contratacion_id);
```

---

## 2. Análisis de Cardinalidades y Restricciones

1. **`perfiles` (1) $\longleftrightarrow$ (0..*) `perfil_habilidades`:**
   - Relación de pertenencia débil / colección de elementos.
   - Clave primaria compuesta `(perfil_id, habilidad)` que impide registrar habilidades duplicadas en el mismo perfil a nivel físico.
2. **`calificaciones`:**
   - La clave foránea lógica a `contrataciones` y los índices en `destinatario_id` optimizan la búsqueda de calificaciones de un usuario para calcular su promedio sin recorrer la tabla entera.
   - La restricción `UNIQUE (contratacion_id, autor_id, destinatario_id)` garantiza que, incluso bajo condiciones de concurrencia, ningún usuario pueda duplicar una calificación.
3. **Baja Lógica vs Integridad Referencial:**
   - Al implementar baja lógica (`activo = false`), los registros de `perfiles` y `calificaciones` nunca se eliminan físicamente (`ON DELETE CASCADE` solo se activa ante limpiezas de pruebas), garantizando auditoría histórica intacta de contrataciones y pagos pasados.

---

## 3. Prevención de Problemas de Consulta $N+1$ (§8)

- **Carga de Perfil con Habilidades:** Al mapear en JPA/Hibernate, la colección `@ElementCollection` de habilidades debe cargarse con `fetch = FetchType.LAZY` por defecto, utilizando consultas con `JOIN FETCH` (o *Entity Graphs*) al momento de requerir el perfil público consolidado.
- **Cálculo de Reputación:** Para evitar traer todas las filas de calificaciones a memoria cuando un perfil tiene cientos de evaluaciones, el repositorio implementa una consulta agregada optimizada:
  ```sql
  SELECT AVG(c.puntaje), COUNT(c.id) FROM calificaciones c WHERE c.destinatario_id = :userId;
  ```
