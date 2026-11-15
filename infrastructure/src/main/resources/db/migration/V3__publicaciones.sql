-- Catálogo jerárquico (Composite): cada fila es una categoría o una habilidad, con su padre.
CREATE TABLE categories (
    id        BIGINT       NOT NULL PRIMARY KEY,
    parent_id BIGINT       NULL,
    name      VARCHAR(100) NOT NULL,
    kind      VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id),
    CONSTRAINT uq_categories_sibling_name UNIQUE (parent_id, name),
    CONSTRAINT ck_categories_kind CHECK (kind IN ('CATEGORY', 'SKILL'))
);

-- Agregado Publicación. status se guarda (aunque sale del historial) para poder filtrar en la búsqueda.
-- version: optimistic locking. Reservar un lugar fuerza el incremento, así dos aceptaciones simultáneas
-- por el último lugar no pueden confirmarse las dos (caso de aceptación 4).
CREATE TABLE publications (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_id        VARCHAR(64)   NOT NULL,
    type             VARCHAR(20)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    title            VARCHAR(150)  NULL,
    description      VARCHAR(2000) NULL,
    category_id      BIGINT        NULL,
    modality         VARCHAR(20)   NULL,
    duration_minutes INT           NULL,
    capacity         INT           NOT NULL,
    base_price       BIGINT        NULL,
    created_at       TIMESTAMP(6)  NOT NULL,
    published_at     TIMESTAMP(6)  NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_publications_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_publications_type CHECK (type IN ('MENTORING', 'SERVICE', 'CHALLENGE')),
    CONSTRAINT ck_publications_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'PAUSED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_publications_modality CHECK (modality IS NULL OR modality IN ('REMOTE', 'ON_SITE')),
    CONSTRAINT ck_publications_capacity CHECK (capacity >= 1),
    CONSTRAINT ck_publications_price CHECK (base_price IS NULL OR base_price >= 0)
);

-- Índices para "mis publicaciones" y para los filtros y órdenes de la búsqueda (siempre sobre PUBLISHED).
CREATE INDEX ix_publications_author_status ON publications (author_id, status);
CREATE INDEX ix_publications_status_published_at ON publications (status, published_at);
CREATE INDEX ix_publications_status_price ON publications (status, base_price);
CREATE INDEX ix_publications_category ON publications (category_id);

CREATE TABLE publication_slots (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publication_id BIGINT       NOT NULL,
    starts_at      TIMESTAMP(6) NOT NULL,
    ends_at        TIMESTAMP(6) NOT NULL,
    occupied       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_slots_publication FOREIGN KEY (publication_id) REFERENCES publications (id) ON DELETE CASCADE,
    CONSTRAINT uq_slots_schedule UNIQUE (publication_id, starts_at, ends_at),
    CONSTRAINT ck_slots_range CHECK (ends_at > starts_at)
);

CREATE TABLE publication_tags (
    publication_id BIGINT      NOT NULL,
    tag            VARCHAR(50) NOT NULL,
    PRIMARY KEY (publication_id, tag),
    CONSTRAINT fk_tags_publication FOREIGN KEY (publication_id) REFERENCES publications (id) ON DELETE CASCADE
);

-- Para el filtro por habilidad (matchea contra las etiquetas).
CREATE INDEX ix_publication_tags_tag ON publication_tags (tag);

-- Historial inmutable de transiciones: solo se insertan filas, nunca se actualizan ni se borran.
CREATE TABLE publication_status_history (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publication_id BIGINT       NOT NULL,
    seq            INT          NOT NULL,
    from_status    VARCHAR(20)  NULL,
    to_status      VARCHAR(20)  NOT NULL,
    actor_id       VARCHAR(64)  NOT NULL,
    occurred_at    TIMESTAMP(6) NOT NULL,
    reason         VARCHAR(500) NULL,
    CONSTRAINT fk_history_publication FOREIGN KEY (publication_id) REFERENCES publications (id),
    CONSTRAINT uq_history_seq UNIQUE (publication_id, seq)
);
