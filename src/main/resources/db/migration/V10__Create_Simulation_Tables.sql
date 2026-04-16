CREATE TABLE IF NOT EXISTS tb_card_brand (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_card_fee_rate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_brand VARCHAR(50) NOT NULL,
    installments INT NOT NULL,
    repasse BOOLEAN NOT NULL,
    rate_percent DECIMAL(5,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tb_card_fee_rate_brand FOREIGN KEY (card_brand) REFERENCES tb_card_brand(id),
    CONSTRAINT uk_tb_card_fee_rate UNIQUE (card_brand, installments, repasse)
);

INSERT INTO tb_card_brand (id, name, active)
VALUES
    ('visa', 'Visa', TRUE),
    ('mastercard', 'Mastercard', TRUE),
    ('elo', 'Elo', TRUE),
    ('amex', 'American Express', TRUE),
    ('hipercard', 'Hipercard', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    active = VALUES(active);

-- Rates by installment (InfinityPay model)
-- 1x 4.20, 2x 6.09, 3x 7.01, 4x 7.91, 5x 8.80, 6x 9.67,
-- 7x 12.59, 8x 13.42, 9x 14.25, 10x 15.06, 11x 15.87, 12x 16.66

INSERT INTO tb_card_fee_rate (card_brand, installments, repasse, rate_percent, active) VALUES
('visa',1,TRUE,4.20,TRUE),('visa',2,TRUE,6.09,TRUE),('visa',3,TRUE,7.01,TRUE),('visa',4,TRUE,7.91,TRUE),('visa',5,TRUE,8.80,TRUE),('visa',6,TRUE,9.67,TRUE),('visa',7,TRUE,12.59,TRUE),('visa',8,TRUE,13.42,TRUE),('visa',9,TRUE,14.25,TRUE),('visa',10,TRUE,15.06,TRUE),('visa',11,TRUE,15.87,TRUE),('visa',12,TRUE,16.66,TRUE),
('visa',1,FALSE,4.20,TRUE),('visa',2,FALSE,6.09,TRUE),('visa',3,FALSE,7.01,TRUE),('visa',4,FALSE,7.91,TRUE),('visa',5,FALSE,8.80,TRUE),('visa',6,FALSE,9.67,TRUE),('visa',7,FALSE,12.59,TRUE),('visa',8,FALSE,13.42,TRUE),('visa',9,FALSE,14.25,TRUE),('visa',10,FALSE,15.06,TRUE),('visa',11,FALSE,15.87,TRUE),('visa',12,FALSE,16.66,TRUE),
('mastercard',1,TRUE,4.20,TRUE),('mastercard',2,TRUE,6.09,TRUE),('mastercard',3,TRUE,7.01,TRUE),('mastercard',4,TRUE,7.91,TRUE),('mastercard',5,TRUE,8.80,TRUE),('mastercard',6,TRUE,9.67,TRUE),('mastercard',7,TRUE,12.59,TRUE),('mastercard',8,TRUE,13.42,TRUE),('mastercard',9,TRUE,14.25,TRUE),('mastercard',10,TRUE,15.06,TRUE),('mastercard',11,TRUE,15.87,TRUE),('mastercard',12,TRUE,16.66,TRUE),
('mastercard',1,FALSE,4.20,TRUE),('mastercard',2,FALSE,6.09,TRUE),('mastercard',3,FALSE,7.01,TRUE),('mastercard',4,FALSE,7.91,TRUE),('mastercard',5,FALSE,8.80,TRUE),('mastercard',6,FALSE,9.67,TRUE),('mastercard',7,FALSE,12.59,TRUE),('mastercard',8,FALSE,13.42,TRUE),('mastercard',9,FALSE,14.25,TRUE),('mastercard',10,FALSE,15.06,TRUE),('mastercard',11,FALSE,15.87,TRUE),('mastercard',12,FALSE,16.66,TRUE),
('elo',1,TRUE,4.20,TRUE),('elo',2,TRUE,6.09,TRUE),('elo',3,TRUE,7.01,TRUE),('elo',4,TRUE,7.91,TRUE),('elo',5,TRUE,8.80,TRUE),('elo',6,TRUE,9.67,TRUE),('elo',7,TRUE,12.59,TRUE),('elo',8,TRUE,13.42,TRUE),('elo',9,TRUE,14.25,TRUE),('elo',10,TRUE,15.06,TRUE),('elo',11,TRUE,15.87,TRUE),('elo',12,TRUE,16.66,TRUE),
('elo',1,FALSE,4.20,TRUE),('elo',2,FALSE,6.09,TRUE),('elo',3,FALSE,7.01,TRUE),('elo',4,FALSE,7.91,TRUE),('elo',5,FALSE,8.80,TRUE),('elo',6,FALSE,9.67,TRUE),('elo',7,FALSE,12.59,TRUE),('elo',8,FALSE,13.42,TRUE),('elo',9,FALSE,14.25,TRUE),('elo',10,FALSE,15.06,TRUE),('elo',11,FALSE,15.87,TRUE),('elo',12,FALSE,16.66,TRUE),
('amex',1,TRUE,4.20,TRUE),('amex',2,TRUE,6.09,TRUE),('amex',3,TRUE,7.01,TRUE),('amex',4,TRUE,7.91,TRUE),('amex',5,TRUE,8.80,TRUE),('amex',6,TRUE,9.67,TRUE),('amex',7,TRUE,12.59,TRUE),('amex',8,TRUE,13.42,TRUE),('amex',9,TRUE,14.25,TRUE),('amex',10,TRUE,15.06,TRUE),('amex',11,TRUE,15.87,TRUE),('amex',12,TRUE,16.66,TRUE),
('amex',1,FALSE,4.20,TRUE),('amex',2,FALSE,6.09,TRUE),('amex',3,FALSE,7.01,TRUE),('amex',4,FALSE,7.91,TRUE),('amex',5,FALSE,8.80,TRUE),('amex',6,FALSE,9.67,TRUE),('amex',7,FALSE,12.59,TRUE),('amex',8,FALSE,13.42,TRUE),('amex',9,FALSE,14.25,TRUE),('amex',10,FALSE,15.06,TRUE),('amex',11,FALSE,15.87,TRUE),('amex',12,FALSE,16.66,TRUE),
('hipercard',1,TRUE,4.20,TRUE),('hipercard',2,TRUE,6.09,TRUE),('hipercard',3,TRUE,7.01,TRUE),('hipercard',4,TRUE,7.91,TRUE),('hipercard',5,TRUE,8.80,TRUE),('hipercard',6,TRUE,9.67,TRUE),('hipercard',7,TRUE,12.59,TRUE),('hipercard',8,TRUE,13.42,TRUE),('hipercard',9,TRUE,14.25,TRUE),('hipercard',10,TRUE,15.06,TRUE),('hipercard',11,TRUE,15.87,TRUE),('hipercard',12,TRUE,16.66,TRUE),
('hipercard',1,FALSE,4.20,TRUE),('hipercard',2,FALSE,6.09,TRUE),('hipercard',3,FALSE,7.01,TRUE),('hipercard',4,FALSE,7.91,TRUE),('hipercard',5,FALSE,8.80,TRUE),('hipercard',6,FALSE,9.67,TRUE),('hipercard',7,FALSE,12.59,TRUE),('hipercard',8,FALSE,13.42,TRUE),('hipercard',9,FALSE,14.25,TRUE),('hipercard',10,FALSE,15.06,TRUE),('hipercard',11,FALSE,15.87,TRUE),('hipercard',12,FALSE,16.66,TRUE)
ON DUPLICATE KEY UPDATE
    rate_percent = VALUES(rate_percent),
    active = VALUES(active);
