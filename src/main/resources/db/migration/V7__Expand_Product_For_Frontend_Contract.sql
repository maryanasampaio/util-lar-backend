ALTER TABLE tb_produto
    ADD COLUMN cost_price BIGINT NULL,
    ADD COLUMN product_condition VARCHAR(10) NOT NULL DEFAULT 'novo',
    ADD COLUMN category VARCHAR(100) NOT NULL DEFAULT 'Geral',
    ADD COLUMN stock INT NOT NULL DEFAULT 0,
    ADD COLUMN dimensions JSON NULL,
    ADD COLUMN material VARCHAR(100) NULL,
    ADD COLUMN color VARCHAR(50) NULL,
    ADD COLUMN brand VARCHAR(100) NULL,
    ADD COLUMN warranty VARCHAR(100) NULL,
    ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sold_date DATETIME(6) NULL;

ALTER TABLE tb_produto
    MODIFY COLUMN description VARCHAR(1000) NOT NULL,
    MODIFY COLUMN images LONGTEXT NULL;

CREATE INDEX idx_tb_produto_category ON tb_produto(category);
CREATE INDEX idx_tb_produto_condition ON tb_produto(product_condition);
CREATE INDEX idx_tb_produto_sold_date ON tb_produto(sold_date);
CREATE INDEX idx_tb_produto_featured ON tb_produto(featured);
