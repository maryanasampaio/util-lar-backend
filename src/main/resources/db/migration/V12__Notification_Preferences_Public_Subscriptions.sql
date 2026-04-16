ALTER TABLE tb_notification_preference
	DROP FOREIGN KEY fk_tb_notification_preference_user;

ALTER TABLE tb_notification_preference
	DROP INDEX uk_tb_notification_preference_user;

ALTER TABLE tb_notification_preference
	DROP COLUMN user_id,
	DROP COLUMN email_enabled,
	DROP COLUMN email_address,
	DROP COLUMN push_enabled,
	DROP COLUMN push_subscription_json;

CREATE UNIQUE INDEX uk_tb_notification_preference_whatsapp_phone
	ON tb_notification_preference(whatsapp_phone);

CREATE INDEX idx_tb_notification_preference_whatsapp_phone
	ON tb_notification_preference(whatsapp_phone);