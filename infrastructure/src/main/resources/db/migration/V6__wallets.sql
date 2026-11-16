CREATE TABLE wallet (
    id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id VARCHAR(100) NOT NULL,
    balance  BIGINT       NOT NULL,
    version  BIGINT       NOT NULL,
    CONSTRAINT uq_wallet_owner UNIQUE (owner_id),
    CONSTRAINT ck_wallet_balance CHECK (balance >= 0)
);

CREATE TABLE wallet_movement (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wallet_id    BIGINT       NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    amount       BIGINT       NOT NULL,
    occurred_at  TIMESTAMP(6) NOT NULL,
    booking_id   BIGINT       NULL,
    CONSTRAINT fk_wallet_movement_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_wallet_movement_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT uq_wallet_booking_movement UNIQUE (wallet_id, booking_id, movement_type),
    CONSTRAINT ck_wallet_movement_type CHECK (movement_type IN ('TOP_UP', 'DEBIT', 'CREDIT', 'REFUND')),
    CONSTRAINT ck_wallet_movement_amount CHECK (amount > 0),
    CONSTRAINT ck_wallet_movement_booking CHECK (
        (movement_type = 'TOP_UP' AND booking_id IS NULL)
        OR (movement_type <> 'TOP_UP' AND booking_id IS NOT NULL)
    )
);

CREATE INDEX idx_wallet_movement_booking ON wallet_movement (booking_id);
