CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    uuid            UUID            NOT NULL DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL,
    username        VARCHAR(50)     NOT NULL,
    display_name    VARCHAR(100),
    email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_uuid     UNIQUE (uuid),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

CREATE UNIQUE INDEX users_username_ci_uk ON users (LOWER(username));
CREATE INDEX idx_users_unverified_cleanup ON users (created_at) WHERE email_verified = false;