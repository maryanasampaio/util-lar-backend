-- 1) Adiciona coluna slug (temporariamente nullable)
ALTER TABLE tb_produto ADD COLUMN slug VARCHAR(255);

-- 2) Backfill: gera slug básico a partir do nome
UPDATE tb_produto SET slug = LOWER(REPLACE(name, ' ', '-')) WHERE slug IS NULL OR slug = '';

-- 3) Define NOT NULL
ALTER TABLE tb_produto MODIFY slug VARCHAR(255) NOT NULL;

-- 4) Índice único para evitar duplicidade
ALTER TABLE tb_produto ADD CONSTRAINT UK_tb_produto_slug UNIQUE (slug);
