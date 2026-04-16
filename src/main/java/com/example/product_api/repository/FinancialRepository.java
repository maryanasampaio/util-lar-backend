package com.example.product_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.product_api.model.Product;

import java.util.List;

@Repository
public interface FinancialRepository extends JpaRepository<Product, Long> {

    /**
     * Métricas do mês especificado
     * Retorna: [itensVendidos, vendas, custo, lucro, margem]
     */
    @Query(value = """
        SELECT 
            COALESCE(COUNT(*), 0) as items_vendidos,
            COALESCE(SUM(price), 0) as total_vendas,
            COALESCE(SUM(cost_price), 0) as total_custo,
            COALESCE(SUM(price - cost_price), 0) as lucro,
            CASE 
                WHEN SUM(price) > 0 THEN ROUND(SUM(price - cost_price) / SUM(price) * 100, 2)
                ELSE 0.0
            END as margem
        FROM tb_produto
        WHERE disponivel = 0
          AND YEAR(sold_date) = :year
          AND MONTH(sold_date) = :month
        """, nativeQuery = true)
    Object[] getMonthMetrics(@Param("year") int year, @Param("month") int month);

    /**
     * Produto mais vendido do mês especificado
     */
    @Query(value = """
        SELECT name
        FROM tb_produto
        WHERE disponivel = 0
          AND YEAR(sold_date) = :year
          AND MONTH(sold_date) = :month
        GROUP BY name
        ORDER BY COUNT(*) DESC
        LIMIT 1
        """, nativeQuery = true)
    String getTopSellingProductOfMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Métricas gerais (histórico completo de vendas)
     * Retorna: [lucroTotal, vendasTotais, itensVendidosTotal, margemMedia]
     */
    @Query(value = """
        SELECT 
            COALESCE(SUM(price - cost_price), 0) as lucro_total,
            COALESCE(SUM(price), 0) as vendas_totais,
            COALESCE(COUNT(*), 0) as items_vendidos_total,
            CASE 
                WHEN SUM(price) > 0 THEN ROUND(SUM(price - cost_price) / SUM(price) * 100, 2)
                ELSE 0.0
            END as margem_media
        FROM tb_produto
        WHERE disponivel = 0
        """, nativeQuery = true)
    Object[] getGeneralMetrics();

    /**
     * Métricas de estoque atual
     * Retorna: [quantidade, valorCusto, valorPotencial, lucroPotencial]
     */
    @Query(value = """
        SELECT 
            COALESCE(COUNT(*), 0) as quantidade,
            COALESCE(SUM(cost_price), 0) as valor_custo,
            COALESCE(SUM(price), 0) as valor_potencial,
            COALESCE(SUM(price - cost_price), 0) as lucro_potencial
        FROM tb_produto
        WHERE disponivel = 1
        """, nativeQuery = true)
    Object[] getStockMetrics();

    /**
     * Quantidade de produtos parados (estoque baixo < 2)
     */
    @Query(value = """
        SELECT COALESCE(COUNT(*), 0)
        FROM tb_produto
        WHERE disponivel = 1 
          AND stock < 2
        """, nativeQuery = true)
    Integer getStuckProductsCount();

    /**
     * Evolução mensal dos últimos N meses
     * Retorna: [mesChave, itensVendidos, vendas, custo, lucro, margem]
     */
    @Query(value = """
        SELECT 
            DATE_FORMAT(sold_date, '%Y-%m') as mes_chave,
            COALESCE(COUNT(*), 0) as items_vendidos,
            COALESCE(SUM(price), 0) as vendas,
            COALESCE(SUM(cost_price), 0) as custo,
            COALESCE(SUM(price - cost_price), 0) as lucro,
            CASE 
                WHEN SUM(price) > 0 THEN ROUND(SUM(price - cost_price) / SUM(price) * 100, 2)
                ELSE 0.0
            END as margem
        FROM tb_produto
        WHERE disponivel = 0
          AND sold_date >= DATE_SUB(CURDATE(), INTERVAL :meses MONTH)
        GROUP BY DATE_FORMAT(sold_date, '%Y-%m')
        ORDER BY mes_chave DESC
        """, nativeQuery = true)
    List<Object[]> getMonthlyEvolution(@Param("meses") int meses);

    /**
     * Top N produtos mais vendidos (por receita total)
     * Retorna: [id, nome, categoria, preco, precoCusto, vezesVendido, receitaTotal, margem]
     */
    @Query(value = """
        SELECT 
            MIN(id) as id,
            name as nome,
            category as categoria,
            AVG(price) as preco,
            AVG(cost_price) as preco_custo,
            COUNT(*) as vezes_vendido,
            SUM(price) as receita_total,
            CASE 
                WHEN AVG(price) > 0 THEN ROUND(AVG((price - cost_price) / price * 100), 2)
                ELSE 0.0
            END as margem
        FROM tb_produto
        WHERE disponivel = 0
        GROUP BY name, category
        ORDER BY receita_total DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getTopProducts(@Param("limit") int limit);

    /**
     * Produtos em estoque com menor margem de lucro
     * Retorna: [id, nome, categoria, preco, precoCusto, margem, estoque]
     */
    @Query(value = """
        SELECT 
            id,
            name as nome,
            category as categoria,
            price as preco,
            cost_price as preco_custo,
            CASE 
                WHEN price > 0 AND cost_price > 0 
                THEN ROUND((price - cost_price) / price * 100, 2)
                ELSE 0.0
            END as margem,
            stock as estoque
        FROM tb_produto
        WHERE disponivel = 1
          AND cost_price > 0
          AND price > 0
        ORDER BY margem ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getLowMarginProducts(@Param("limit") int limit);

    /**
     * Produtos parados (estoque baixo)
     * Retorna: [id, nome, categoria, preco, precoCusto, margem, estoque]
     */
    @Query(value = """
        SELECT 
            id,
            name as nome,
            category as categoria,
            price as preco,
            cost_price as preco_custo,
            CASE 
                WHEN price > 0 AND cost_price > 0 
                THEN ROUND((price - cost_price) / price * 100, 2)
                ELSE 0.0
            END as margem,
            stock as estoque
        FROM tb_produto
        WHERE disponivel = 1 
          AND stock < 2
        ORDER BY stock ASC, name ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getStuckProducts(@Param("limit") int limit);

    /**
     * Produtos vendidos por mês/ano
     * Retorna: [id, nome, categoria, preco, soldDate]
     */
    @Query(value = """
        SELECT
            id,
            name as nome,
            category as categoria,
            price as preco,
            sold_date as sold_date
        FROM tb_produto
        WHERE disponivel = 0
          AND sold_date IS NOT NULL
          AND YEAR(sold_date) = :year
          AND MONTH(sold_date) = :month
        ORDER BY sold_date DESC, id DESC
        """, nativeQuery = true)
    List<Object[]> getSoldProductsByMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT DATABASE()", nativeQuery = true)
    String getCurrentDatabase();

    @Query(value = """
        SELECT
            COALESCE(COUNT(*), 0) as vendidos_mes,
            COALESCE(SUM(price), 0) as vendas_mes,
            COALESCE(SUM(cost_price), 0) as custo_mes,
            COALESCE(SUM(price - cost_price), 0) as lucro_mes
        FROM tb_produto
        WHERE disponivel = 0
          AND sold_date IS NOT NULL
          AND YEAR(sold_date) = :year
          AND MONTH(sold_date) = :month
        """, nativeQuery = true)
    Object[] getMonthMetricsRaw(@Param("year") int year, @Param("month") int month);

    @Query(value = """
        SELECT
            COALESCE(COUNT(*), 0) as estoque_qtd,
            COALESCE(SUM(cost_price), 0) as estoque_custo,
            COALESCE(SUM(price), 0) as estoque_potencial
        FROM tb_produto
        WHERE disponivel = 1
        """, nativeQuery = true)
    Object[] getStockMetricsRaw();
}
