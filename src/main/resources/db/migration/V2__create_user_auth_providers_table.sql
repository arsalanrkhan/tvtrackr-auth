CREATE TABLE auth.user_auth_providers (
    id            BIGSERIAL       PRIMARY KEY,
    user_id       BIGINT          NOT NULL,
    provider      VARCHAR(20)     NOT NULL,
    provider_id   VARCHAR(255),
    password_hash VARCHAR(255),
    version       BIGINT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_auth_providers_user
        FOREIGN KEY (user_id) REFERENCES auth.users (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_provider
        UNIQUE (user_id, provider),

    CONSTRAINT chk_provider_type
        CHECK (provider IN ('LOCAL', 'GOOGLE'))
);