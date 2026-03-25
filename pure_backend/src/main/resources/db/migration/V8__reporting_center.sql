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
