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
