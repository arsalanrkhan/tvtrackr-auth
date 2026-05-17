CREATE TABLE auth.refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    uuid        UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id     BIGINT          NOT NULL,
    token       VARCHAR(512)    NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMP       NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_tokens_uuid  UNIQUE (uuid),
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES auth.users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id  ON auth.refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token    ON auth.refresh_tokens (token);