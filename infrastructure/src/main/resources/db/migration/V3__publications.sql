-- Publicaciones, sus franjas, etiquetas e historial inmutable de estados, y el catálogo de categorías.

-- Catálogo jerárquico (Composite): cada fila es una categoría o una habilidad, con su padre.
CREATE TABLE category (
    id        BIGINT       NOT NULL PRIMARY KEY,
    parent_id BIGINT       NULL,
    name      VARCHAR(100) NOT NULL,
    kind      VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id),
    CONSTRAINT uq_category_sibling_name UNIQUE (parent_id, name),
    CONSTRAINT ck_category_kind CHECK (kind IN ('CATEGORY', 'SKILL'))
);

-- status se guarda para filtrar en la búsqueda; version es el optimistic locking del cupo (caso 4).
CREATE TABLE publication (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_id        VARCHAR(100)  NOT NULL,
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
    CONSTRAINT fk_publication_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT ck_publication_type CHECK (type IN ('MENTORING', 'SERVICE', 'CHALLENGE')),
    CONSTRAINT ck_publication_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'PAUSED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_publication_modality CHECK (modality IS NULL OR modality IN ('REMOTE', 'ON_SITE')),
    CONSTRAINT ck_publication_capacity CHECK (capacity >= 1),
    CONSTRAINT ck_publication_price CHECK (base_price IS NULL OR base_price >= 0)
);

CREATE INDEX idx_publication_author_status ON publication (author_id, status);
CREATE INDEX idx_publication_status_published_at ON publication (status, published_at);
CREATE INDEX idx_publication_status_price ON publication (status, base_price);
CREATE INDEX idx_publication_category ON publication (category_id);

CREATE TABLE publication_slot (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publication_id BIGINT       NOT NULL,
    starts_at      TIMESTAMP(6) NOT NULL,
    ends_at        TIMESTAMP(6) NOT NULL,
    occupied       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_publication_slot_publication FOREIGN KEY (publication_id) REFERENCES publication (id) ON DELETE CASCADE,
    CONSTRAINT uq_publication_slot_schedule UNIQUE (publication_id, starts_at, ends_at),
    CONSTRAINT ck_publication_slot_range CHECK (ends_at > starts_at)
);

CREATE TABLE publication_tag (
    publication_id BIGINT      NOT NULL,
    tag            VARCHAR(50) NOT NULL,
    PRIMARY KEY (publication_id, tag),
    CONSTRAINT fk_publication_tag_publication FOREIGN KEY (publication_id) REFERENCES publication (id) ON DELETE CASCADE
);

CREATE INDEX idx_publication_tag_tag ON publication_tag (tag);

CREATE TABLE publication_transition (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publication_id BIGINT       NOT NULL,
    position       INT          NOT NULL,
    from_status    VARCHAR(20)  NULL,
    to_status      VARCHAR(20)  NOT NULL,
    actor_id       VARCHAR(100) NOT NULL,
    occurred_at    TIMESTAMP(6) NOT NULL,
    reason         VARCHAR(500) NULL,
    CONSTRAINT fk_publication_transition_publication FOREIGN KEY (publication_id) REFERENCES publication (id),
    CONSTRAINT uq_publication_transition_position UNIQUE (publication_id, position)
);
