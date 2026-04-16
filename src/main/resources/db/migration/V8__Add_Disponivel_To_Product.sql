ALTER TABLE tb_produto
    ADD COLUMN disponivel TINYINT NOT NULL DEFAULT 1;

UPDATE tb_produto
SET disponivel = CASE
    WHEN sold_date IS NULL THEN 1
    ELSE 0
END;
