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
