CREATE TABLE click_events (
    id              BIGSERIAL PRIMARY KEY,
    short_url_id    BIGINT NOT NULL REFERENCES short_urls(id),
    clicked_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    referrer        TEXT NULL
);

CREATE INDEX idx_click_events_short_url_id ON click_events (short_url_id);
