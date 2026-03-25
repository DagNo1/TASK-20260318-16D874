CREATE TABLE notification_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(80) NOT NULL,
    payload_json JSON NOT NULL,
    actor_user_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notification_event_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE TABLE notification_subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notification_sub_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_notification_sub UNIQUE (user_id, event_type)
);

CREATE TABLE notification_deliveries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    delivered_at DATETIME(6) NOT NULL,
    read_at DATETIME(6),
    CONSTRAINT fk_notification_delivery_event FOREIGN KEY (event_id) REFERENCES notification_events(id),
    CONSTRAINT fk_notification_delivery_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id)
);

CREATE INDEX idx_notification_delivery_user_time ON notification_deliveries (recipient_user_id, delivered_at DESC);
CREATE INDEX idx_notification_delivery_unread ON notification_deliveries (recipient_user_id, read_at);
