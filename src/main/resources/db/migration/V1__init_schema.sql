CREATE TABLE short_urls (
    id              BIGSERIAL PRIMARY KEY,
    original_url    TEXT NOT NULL,
    short_code      VARCHAR(12) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NULL,
    click_count     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_short_urls_short_code UNIQUE (short_code)
);

CREATE INDEX idx_short_urls_original_url ON short_urls (original_url);
