CREATE TABLE IF NOT EXISTS tb_notification_preference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    whatsapp_phone VARCHAR(20),
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_address VARCHAR(255),
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    push_subscription_json LONGTEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_tb_notification_preference_user UNIQUE (user_id),
    CONSTRAINT fk_tb_notification_preference_user FOREIGN KEY (user_id) REFERENCES tb_usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tb_notification_preference_whatsapp_enabled
    ON tb_notification_preference(whatsapp_enabled);

CREATE INDEX idx_tb_notification_preference_email_enabled
    ON tb_notification_preference(email_enabled);

CREATE INDEX idx_tb_notification_preference_push_enabled
    ON tb_notification_preference(push_enabled);
