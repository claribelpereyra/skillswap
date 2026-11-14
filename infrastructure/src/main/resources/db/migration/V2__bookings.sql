-- Reservas (contrataciones) y su historial inmutable de estados.
-- La franja reservada se copia como foto (id, inicio y fin), igual que el precio.

CREATE TABLE booking (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publication_id          BIGINT       NOT NULL,
    client_id               VARCHAR(100) NOT NULL,
    provider_id             VARCHAR(100) NOT NULL,
    slot_id                 BIGINT       NOT NULL,
    slot_starts_at          TIMESTAMP(6) NOT NULL,
    slot_ends_at            TIMESTAMP(6) NOT NULL,
    message                 VARCHAR(1000) NOT NULL,
    goal                    VARCHAR(500) NOT NULL,
    price                   BIGINT       NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    proposed_slot_id        BIGINT       NULL,
    proposed_slot_starts_at TIMESTAMP(6) NULL,
    proposed_slot_ends_at   TIMESTAMP(6) NULL,
    reschedule_proposed_by  VARCHAR(100) NULL,
    version                 BIGINT       NOT NULL
);

CREATE INDEX idx_booking_client ON booking (client_id);
CREATE INDEX idx_booking_provider ON booking (provider_id);
CREATE INDEX idx_booking_publication_slot ON booking (publication_id, slot_id);

CREATE TABLE booking_transition (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT       NOT NULL,
    position    INT          NOT NULL,
    from_status VARCHAR(20)  NULL,
    to_status   VARCHAR(20)  NOT NULL,
    actor_id    VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    reason      VARCHAR(500) NULL,
    CONSTRAINT fk_booking_transition_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT uq_booking_transition_position UNIQUE (booking_id, position)
);
