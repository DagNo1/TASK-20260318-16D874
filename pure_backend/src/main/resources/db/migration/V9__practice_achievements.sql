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
