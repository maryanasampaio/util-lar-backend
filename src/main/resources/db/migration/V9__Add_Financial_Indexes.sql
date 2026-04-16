-- Migration: Adicionar índices compostos para otimização de queries financeiras
-- Autor: Sistema
-- Data: 2026-03-02

-- Índice composto para queries de vendas por data
-- Otimiza: WHERE disponivel = 0 AND YEAR(sold_date) = X AND MONTH(sold_date) = Y
CREATE INDEX idx_disponivel_sold_date ON tb_produto(disponivel, sold_date);

-- Índice composto para queries de estoque
-- Otimiza: WHERE disponivel = 1 AND stock < 2
CREATE INDEX idx_disponivel_stock ON tb_produto(disponivel, stock);

-- Índice para ordenação por margem de lucro em produtos disponíveis
-- Otimiza: WHERE disponivel = 1 ORDER BY (price - cost_price) / price
CREATE INDEX idx_disponivel_price_cost ON tb_produto(disponivel, price, cost_price);

-- Índice para agregações por nome de produto (cálculo de receita por produto)
-- Otimiza: GROUP BY name, category WHERE disponivel = 0
CREATE INDEX idx_name_category_disponivel ON tb_produto(name, category, disponivel);
