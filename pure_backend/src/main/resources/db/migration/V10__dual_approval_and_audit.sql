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
