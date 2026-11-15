CREATE TABLE wallet (
    owner_id VARCHAR(100) NOT NULL PRIMARY KEY,
    balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_wallet_balance CHECK (balance >= 0)
);

CREATE TABLE wallet_movement (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wallet_owner_id VARCHAR(100) NOT NULL,
    seq INT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    booking_id BIGINT NULL,
    CONSTRAINT fk_wallet_movement_wallet FOREIGN KEY (wallet_owner_id)
        REFERENCES wallet (owner_id),
    CONSTRAINT fk_wallet_movement_booking FOREIGN KEY (booking_id)
        REFERENCES booking (id),
    CONSTRAINT uq_wallet_movement_sequence UNIQUE (wallet_owner_id, seq),
    CONSTRAINT ck_wallet_movement_sequence CHECK (seq >= 0),
    CONSTRAINT ck_wallet_movement_type CHECK (movement_type IN ('TOP_UP', 'DEBIT', 'CREDIT', 'REFUND')),
    CONSTRAINT ck_wallet_movement_amount CHECK (amount > 0),
    CONSTRAINT ck_wallet_movement_booking CHECK (
        (movement_type = 'TOP_UP' AND booking_id IS NULL)
        OR (movement_type <> 'TOP_UP' AND booking_id IS NOT NULL AND booking_id > 0)
    )
);

CREATE INDEX idx_wallet_movement_booking ON wallet_movement (booking_id);
