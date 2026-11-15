-- Profiles, skills, interests and verified post-booking ratings.

CREATE TABLE profile (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      VARCHAR(100)  NOT NULL,
    display_name VARCHAR(150)  NOT NULL,
    bio          VARCHAR(2000) NOT NULL DEFAULT '',
    time_zone    VARCHAR(80)   NOT NULL,
    account_type VARCHAR(30)   NOT NULL,
    reputation   DOUBLE        NOT NULL DEFAULT 0.0,
    rating_sum   INT           NOT NULL DEFAULT 0,
    rating_count INT           NOT NULL DEFAULT 0,
    availability VARCHAR(500)  NOT NULL DEFAULT 'Not specified',
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(6)  NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_profile_user_id UNIQUE (user_id),
    CONSTRAINT ck_profile_account_type CHECK (account_type IN ('PROFESSIONAL', 'ORGANIZATION')),
    CONSTRAINT ck_profile_reputation CHECK (reputation >= 0.0 AND reputation <= 5.0),
    CONSTRAINT ck_profile_rating_sum CHECK (rating_sum >= 0),
    CONSTRAINT ck_profile_rating_count CHECK (rating_count >= 0)
);

CREATE TABLE profile_skill (
    profile_id BIGINT NOT NULL,
    skill_id   BIGINT NOT NULL,
    PRIMARY KEY (profile_id, skill_id),
    CONSTRAINT fk_profile_skill_profile
        FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE
);

CREATE TABLE profile_interest (
    profile_id BIGINT       NOT NULL,
    interest   VARCHAR(120) NOT NULL,
    PRIMARY KEY (profile_id, interest),
    CONSTRAINT fk_profile_interest_profile
        FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE
);

CREATE TABLE rating (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id   BIGINT        NOT NULL,
    author_id    VARCHAR(100)  NOT NULL,
    recipient_id VARCHAR(100)  NOT NULL,
    score        INT           NOT NULL,
    comment      VARCHAR(1000) NOT NULL DEFAULT '',
    rated_at     TIMESTAMP(6)  NOT NULL,
    CONSTRAINT fk_rating_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT uq_rating_booking_author UNIQUE (booking_id, author_id),
    CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT ck_rating_not_self CHECK (author_id <> recipient_id)
);

CREATE INDEX idx_rating_recipient_rated_at ON rating (recipient_id, rated_at);
