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
