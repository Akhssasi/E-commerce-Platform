CREATE TABLE outbox_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(36) NOT NULL,
    event_type   VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    payload      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts     INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);

CREATE TABLE notification_log (
    order_no VARCHAR(32)  PRIMARY KEY,
    email    VARCHAR(160) NOT NULL,
    sent_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
