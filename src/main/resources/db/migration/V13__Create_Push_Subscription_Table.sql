CREATE TABLE IF NOT EXISTS tb_push_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT,
    endpoint VARCHAR(1000) NOT NULL,
    public_key VARCHAR(512) NOT NULL,
    auth_secret VARCHAR(255) NOT NULL,
    expiration_time BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_tb_push_subscription_endpoint UNIQUE (endpoint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tb_push_subscription_created_at
    ON tb_push_subscription(created_at);