CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE practice_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE practice_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    instruction TEXT,
    estimated_seconds BIGINT,
    status VARCHAR(40) NOT NULL,
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_step_session FOREIGN KEY (session_id) REFERENCES practice_sessions(id),
    CONSTRAINT uq_session_step_order UNIQUE (session_id, step_order)
);

CREATE TABLE step_timers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    step_id BIGINT NOT NULL,
    timer_key VARCHAR(120) NOT NULL,
    duration_seconds BIGINT NOT NULL,
    remaining_seconds BIGINT NOT NULL,
    state VARCHAR(40) NOT NULL,
    started_at DATETIME(6),
    last_resumed_at DATETIME(6),
    last_paused_at DATETIME(6),
    due_at DATETIME(6),
    completed_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_timer_step FOREIGN KEY (step_id) REFERENCES practice_steps(id),
    CONSTRAINT uq_timer_step_key UNIQUE (step_id, timer_key)
);

CREATE TABLE step_reminders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timer_id BIGINT NOT NULL,
    offset_seconds_before_due BIGINT NOT NULL,
    remind_at DATETIME(6),
    message VARCHAR(500) NOT NULL,
    triggered BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at DATETIME(6),
    CONSTRAINT fk_reminder_timer FOREIGN KEY (timer_id) REFERENCES step_timers(id)
);

CREATE TABLE session_checkpoints (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    checkpoint_type VARCHAR(40) NOT NULL,
    snapshot_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_checkpoint_session FOREIGN KEY (session_id) REFERENCES practice_sessions(id)
);

CREATE INDEX idx_checkpoint_session_created ON session_checkpoints (session_id, created_at DESC);
CREATE INDEX idx_reminder_due_triggered ON step_reminders (triggered, remind_at);
CREATE INDEX idx_timer_state_due ON step_timers (state, due_at);

CREATE TABLE technique_cards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE technique_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE technique_card_tags (
    card_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (card_id, tag_id),
    CONSTRAINT fk_card_tag_card FOREIGN KEY (card_id) REFERENCES technique_cards(id),
    CONSTRAINT fk_card_tag_tag FOREIGN KEY (tag_id) REFERENCES technique_tags(id)
);

CREATE TABLE step_technique_cards (
    step_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    PRIMARY KEY (step_id, card_id),
    CONSTRAINT fk_step_card_step FOREIGN KEY (step_id) REFERENCES practice_steps(id),
    CONSTRAINT fk_step_card_card FOREIGN KEY (card_id) REFERENCES technique_cards(id)
);

ALTER TABLE practice_steps
    ADD COLUMN active_elapsed_seconds BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN active_since DATETIME(6) NULL;

CREATE TABLE step_node_reminders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    step_id BIGINT NOT NULL,
    offset_seconds_after_step_start BIGINT NOT NULL,
    remind_at DATETIME(6),
    message VARCHAR(500) NOT NULL,
    triggered BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at DATETIME(6),
    CONSTRAINT fk_node_reminder_step FOREIGN KEY (step_id) REFERENCES practice_steps(id)
);

CREATE INDEX idx_node_reminder_due_triggered ON step_node_reminders (triggered, remind_at);
CREATE INDEX idx_node_reminder_step ON step_node_reminders (step_id);

CREATE TABLE im_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_im_session_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE im_session_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT,
    last_read_at DATETIME(6),
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_im_member_session FOREIGN KEY (session_id) REFERENCES im_sessions(id),
    CONSTRAINT fk_im_member_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_im_member UNIQUE (session_id, user_id)
);

CREATE TABLE im_image_assets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fingerprint VARCHAR(128) NOT NULL UNIQUE,
    mime_type VARCHAR(40) NOT NULL,
    size_bytes BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_im_image_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE im_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    content TEXT,
    image_asset_id BIGINT,
    dedup_key VARCHAR(128),
    folded_count INT NOT NULL DEFAULT 1,
    last_folded_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_im_msg_session FOREIGN KEY (session_id) REFERENCES im_sessions(id),
    CONSTRAINT fk_im_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_im_msg_image FOREIGN KEY (image_asset_id) REFERENCES im_image_assets(id)
);

ALTER TABLE im_session_members
    ADD CONSTRAINT fk_im_member_last_read FOREIGN KEY (last_read_message_id) REFERENCES im_messages(id);

CREATE INDEX idx_im_msg_session_created ON im_messages (session_id, created_at DESC);
CREATE INDEX idx_im_msg_sender_dedup_window ON im_messages (session_id, sender_id, dedup_key, created_at DESC);

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

CREATE TABLE inventory_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_user_id BIGINT NOT NULL,
    sku VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    stock_quantity BIGINT NOT NULL,
    alert_threshold BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inventory_item_merchant FOREIGN KEY (merchant_user_id) REFERENCES users(id),
    CONSTRAINT uq_inventory_item_merchant_sku UNIQUE (merchant_user_id, sku)
);

CREATE TABLE inventory_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    previous_quantity BIGINT NOT NULL,
    new_quantity BIGINT NOT NULL,
    change_reason VARCHAR(255),
    changed_by_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inventory_log_item FOREIGN KEY (item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_inventory_log_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);

CREATE TABLE inventory_alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    threshold_value BIGINT NOT NULL,
    stock_quantity BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6),
    CONSTRAINT fk_inventory_alert_item FOREIGN KEY (item_id) REFERENCES inventory_items(id)
);

CREATE INDEX idx_inventory_alert_item_status ON inventory_alerts (item_id, status, created_at DESC);
CREATE INDEX idx_inventory_alert_created ON inventory_alerts (created_at DESC);

CREATE TABLE report_indicator_definitions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE report_generations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    generated_for_date DATE NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    generated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_report_generation UNIQUE (generated_for_date, timezone)
);

CREATE TABLE report_aggregates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    generation_id BIGINT NOT NULL,
    indicator_code VARCHAR(80) NOT NULL,
    organization_user_id BIGINT NOT NULL,
    business_dimension VARCHAR(60) NOT NULL,
    dimension_value VARCHAR(255) NOT NULL,
    period_start DATETIME(6) NOT NULL,
    period_end DATETIME(6) NOT NULL,
    aggregated_value DECIMAL(20,4) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_report_agg_generation FOREIGN KEY (generation_id) REFERENCES report_generations(id)
);

CREATE INDEX idx_report_agg_search
    ON report_aggregates (indicator_code, organization_user_id, business_dimension, period_start, period_end);

INSERT INTO report_indicator_definitions (code, name, description, enabled)
VALUES
    ('INVENTORY_CHANGE_EVENTS', 'Inventory Change Events', 'Count of inventory log records', TRUE),
    ('INVENTORY_NET_STOCK_DELTA', 'Inventory Net Stock Delta', 'Sum of (new_quantity - previous_quantity) across inventory logs', TRUE),
    ('LOW_STOCK_ALERT_EVENTS', 'Low Stock Alert Events', 'Count of inventory alerts created in period', TRUE);

CREATE TABLE practice_achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    version_no BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    responsible_person VARCHAR(255) NOT NULL,
    conclusion TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_achievement_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE TABLE practice_achievement_attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    achievement_id BIGINT NOT NULL,
    attachment_version BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_achievement_attachment_achievement FOREIGN KEY (achievement_id) REFERENCES practice_achievements(id),
    CONSTRAINT uq_achievement_attachment_ver UNIQUE (achievement_id, attachment_version)
);

CREATE TABLE practice_assessment_forms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    achievement_id BIGINT NOT NULL,
    assessor_name VARCHAR(255) NOT NULL,
    assessed_at DATETIME(6) NOT NULL,
    overall_score DECIMAL(8,2) NOT NULL,
    strengths TEXT,
    improvements TEXT,
    recommendations TEXT,
    criteria_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_assessment_achievement FOREIGN KEY (achievement_id) REFERENCES practice_achievements(id),
    CONSTRAINT uq_assessment_achievement UNIQUE (achievement_id)
);

CREATE INDEX idx_achievement_owner_updated ON practice_achievements (owner_user_id, updated_at DESC);

CREATE TABLE approval_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    target_resource VARCHAR(120) NOT NULL,
    target_action VARCHAR(120) NOT NULL,
    payload_json JSON NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    decided_at DATETIME(6),
    executed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_approval_request_user FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
);

CREATE TABLE approval_decisions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    approval_request_id BIGINT NOT NULL,
    admin_user_id BIGINT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comment VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_approval_decision_request FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id),
    CONSTRAINT fk_approval_decision_admin FOREIGN KEY (admin_user_id) REFERENCES users(id),
    CONSTRAINT uq_approval_decision_admin UNIQUE (approval_request_id, admin_user_id)
);

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(80) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    approval_request_id BIGINT,
    details_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
    CONSTRAINT fk_audit_approval_request FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id)
);

CREATE INDEX idx_approval_status_created ON approval_requests (status, created_at DESC);
CREATE INDEX idx_audit_event_time ON audit_logs (event_type, created_at DESC);

INSERT INTO roles (name) VALUES ('ROLE_USER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');

INSERT INTO users (username, password_hash, enabled)
VALUES ('admin', '$2y$10$ySLKRrNWEW9Uhiyc.p7Wl.ai/YCm63GPSQz46JIUPR5BYPsIcaOgy', TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username = 'admin';

INSERT INTO roles (name)
SELECT 'ROLE_PLATFORM_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_PLATFORM_ADMIN');

INSERT INTO roles (name)
SELECT 'ROLE_MERCHANT_OPERATOR'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MERCHANT_OPERATOR');

INSERT INTO roles (name)
SELECT 'ROLE_REGULAR_BUYER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_REGULAR_BUYER');

INSERT INTO roles (name)
SELECT 'ROLE_REVIEWER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_REVIEWER');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_PLATFORM_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- Default seeded users (password for all: "password")
-- admin_user -> ROLE_ADMIN
-- basic_user -> ROLE_USER
-- platform_admin -> ROLE_PLATFORM_ADMIN
-- merchant_operator -> ROLE_MERCHANT_OPERATOR
-- regular_buyer -> ROLE_REGULAR_BUYER
-- reviewer -> ROLE_REVIEWER

INSERT INTO users (username, password_hash, enabled)
SELECT 'admin_user', '$2y$10$ySLKRrNWEW9Uhiyc.p7Wl.ai/YCm63GPSQz46JIUPR5BYPsIcaOgy', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin_user');

INSERT INTO users (username, password_hash, enabled)
SELECT 'basic_user', '$2y$10$ySLKRrNWEW9Uhiyc.p7Wl.ai/YCm63GPSQz46JIUPR5BYPsIcaOgy', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'basic_user');

INSERT INTO users (username, password_hash, enabled)
SELECT 'platform_admin', '$2y$10$ySLKRrNWEW9Uhiyc.p7Wl.ai/YCm63GPSQz46JIUPR5BYPsIcaOgy', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'platform_admin');

INSERT INTO users (username, password_hash, enabled)
SELECT 'merchant_operator', '$2y$10$ySLKRrNWEW9Uhiyc.p7Wl.ai/YCm63GPSQz46JIUPR5BYPsIcaOgy', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'merchant_operator');

INSERT INTO users (username, password_hash, enabled)
SELECT 'regular_buyer', '$2y$10$ySLKRrNWEW9Uhiyc.p7Wl.ai/YCm63GPSQz46JIUPR5BYPsIcaOgy', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'regular_buyer');

INSERT INTO users (username, password_hash, enabled)
SELECT 'reviewer', '$2y$10$ySLKRrNWEW9Uhiyc.p7Wl.ai/YCm63GPSQz46JIUPR5BYPsIcaOgy', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'reviewer');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin_user'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username = 'basic_user'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_PLATFORM_ADMIN'
WHERE u.username = 'platform_admin'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_MERCHANT_OPERATOR'
WHERE u.username = 'merchant_operator'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_REGULAR_BUYER'
WHERE u.username = 'regular_buyer'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_REVIEWER'
WHERE u.username = 'reviewer'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
