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
