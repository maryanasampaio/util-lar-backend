package com.example.product_api.service;

import com.example.product_api.dto.FinancialDTO.*;
import com.example.product_api.repository.FinancialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialService {
    
    private final FinancialRepository financialRepository;
    @Value("${spring.datasource.url:undefined}")
    private String datasourceUrl;
    private static final Locale PT_BR = new Locale("pt", "BR");

    /**
     * Retorna dashboard completo (mês atual + métricas gerais + estoque)
     * Cache: 5 minutos
     */
    @Cacheable(value = "dashboard", unless = "#result == null")
    public DashboardResponseDTO getDashboard() {
        return buildDashboard();
    }

    public DashboardResponseDTO getDashboardFresh() {
        return buildDashboard();
    }

    private DashboardResponseDTO buildDashboard() {
        log.info("Calculando métricas do dashboard financeiro");
        
        DashboardResponseDTO dashboard = new DashboardResponseDTO();
        
        // Métricas do mês atual
        dashboard.setMesAtual(runSafely("métricas do mês atual", this::getCurrentMonthMetrics, this::emptyCurrentMonthMetrics));
        
        // Métricas gerais (histórico)
        dashboard.setGeral(runSafely("métricas gerais", this::getGeneralMetrics, this::emptyGeneralMetrics));
        
        // Métricas de estoque
        dashboard.setEstoque(runSafely("métricas de estoque", this::getStockMetrics, this::emptyStockMetrics));
        
        return dashboard;
    }

    /**
     * Métricas do mês atual
     */
    private CurrentMonthMetricsDTO getCurrentMonthMetrics() {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        // Buscar dados do mês atual
        Object[] currentData = normalizeRow(financialRepository.getMonthMetrics(currentYear, currentMonth));
        
        CurrentMonthMetricsDTO dto = new CurrentMonthMetricsDTO();
        
        if (currentData != null && currentData.length >= 5) {
            dto.setItensVendidos(toInteger(currentData[0]));
            dto.setVendas(toLong(currentData[1]));
            dto.setCusto(toLong(currentData[2]));
            dto.setLucro(toLong(currentData[3]));
            dto.setMargem(toDouble(currentData[4]));
            
            // Calcular ticket médio
            if (dto.getItensVendidos() != null && dto.getItensVendidos() > 0 && dto.getVendas() != null) {
                dto.setTicketMedio(dto.getVendas() / dto.getItensVendidos());
            } else {
                dto.setTicketMedio(0L);
            }
        } else {
            // Valores padrão se não houver vendas no mês
            dto.setItensVendidos(0);
            dto.setVendas(0L);
            dto.setCusto(0L);
            dto.setLucro(0L);
            dto.setMargem(0.0);
            dto.setTicketMedio(0L);
        }
        
        // Produto mais vendido do mês
        String topProduct = financialRepository.getTopSellingProductOfMonth(currentYear, currentMonth);
        dto.setProdutoMaisVendido(topProduct != null ? topProduct : "");
        
        // Calcular variação vs mês anterior
        LocalDate lastMonth = now.minusMonths(1);
        Object[] lastMonthData = normalizeRow(financialRepository.getMonthMetrics(lastMonth.getYear(), lastMonth.getMonthValue()));
        
        if (lastMonthData != null && lastMonthData.length >= 4) {
            Long currentProfit = dto.getLucro();
            Long lastProfit = toLong(lastMonthData[3]);
            
            if (lastProfit != null && lastProfit > 0 && currentProfit != null) {
                double variation = ((currentProfit - lastProfit) / (double) lastProfit) * 100;
                dto.setVariacaoLucro(Math.round(variation * 100.0) / 100.0);
            } else if (currentProfit != null && currentProfit > 0) {
                // Se não havia vendas no mês anterior mas há no atual, variação é 100%
                dto.setVariacaoLucro(100.0);
            } else {
                dto.setVariacaoLucro(0.0);
            }
        } else {
            dto.setVariacaoLucro(0.0);
        }
        
        return dto;
    }

    /**
     * Métricas gerais (histórico completo)
     */
    private GeneralMetricsDTO getGeneralMetrics() {
        Object[] data = normalizeRow(financialRepository.getGeneralMetrics());
        
        GeneralMetricsDTO dto = new GeneralMetricsDTO();
        
        if (data != null && data.length >= 4) {
            dto.setLucroTotal(toLong(data[0]));
            dto.setVendasTotais(toLong(data[1]));
            dto.setItensVendidosTotal(toInteger(data[2]));
            dto.setMargemMedia(toDouble(data[3]));
        } else {
            dto.setLucroTotal(0L);
            dto.setVendasTotais(0L);
            dto.setItensVendidosTotal(0);
            dto.setMargemMedia(0.0);
        }
        
        return dto;
    }

    /**
     * Métricas de estoque
     */
    private StockMetricsDTO getStockMetrics() {
        Object[] data = normalizeRow(financialRepository.getStockMetrics());
        Integer stuckCount = financialRepository.getStuckProductsCount();
        
        StockMetricsDTO dto = new StockMetricsDTO();
        
        if (data != null && data.length >= 4) {
            dto.setQuantidade(toInteger(data[0]));
            dto.setValorCusto(toLong(data[1]));
            dto.setValorPotencial(toLong(data[2]));
            dto.setLucroPotencial(toLong(data[3]));
        } else {
            dto.setQuantidade(0);
            dto.setValorCusto(0L);
            dto.setValorPotencial(0L);
            dto.setLucroPotencial(0L);
        }
        
        dto.setProdutosParados(stuckCount != null ? stuckCount : 0);
        
        return dto;
    }

    /**
     * Evolução mensal dos últimos N meses
     * Cache: 5 minutos
     */
    @Cacheable(value = "monthlyEvolution", key = "#meses", unless = "#result == null")
    public EvolutionResponseDTO getMonthlyEvolution(int meses) {
        return buildMonthlyEvolution(meses);
    }

    public EvolutionResponseDTO getMonthlyEvolutionFresh(int meses) {
        return buildMonthlyEvolution(meses);
    }

    private EvolutionResponseDTO buildMonthlyEvolution(int meses) {
        return runSafely("evolução mensal", () -> {
            log.info("Calculando evolução mensal dos últimos {} meses", meses);

            List<Object[]> results = financialRepository.getMonthlyEvolution(meses);

            List<MonthlyStatsDTO> monthlyStats = new ArrayList<>();
            Long maxProfit = 1L; // Mínimo 1 para evitar divisão por zero em gráficos

            for (Object[] row : results) {
                if (row == null || row.length < 6) continue;

                MonthlyStatsDTO dto = new MonthlyStatsDTO();
                dto.setMesChave(toString(row[0]));
                dto.setMes(formatMonthDisplay(toString(row[0])));
                dto.setItensVendidos(toInteger(row[1]));
                dto.setVendas(toLong(row[2]));
                dto.setCusto(toLong(row[3]));
                dto.setLucro(toLong(row[4]));
                dto.setMargem(toDouble(row[5]));

                monthlyStats.add(dto);

                // Atualizar lucro máximo
                if (dto.getLucro() != null && dto.getLucro() > maxProfit) {
                    maxProfit = dto.getLucro();
                }
            }

            EvolutionResponseDTO response = new EvolutionResponseDTO();
            response.setMeses(monthlyStats);
            response.setLucroMaximoMes(maxProfit);

            return response;
        }, this::emptyEvolutionResponse);
    }

    /**
     * Produtos por tipo (top, baixa-margem, parados)
     * Cache: 10 minutos
     */
    @Cacheable(value = "products", key = "#tipo + '_' + #limit", unless = "#result == null")
    public ProductsResponseDTO getProducts(String tipo, int limit) {
        return buildProducts(tipo, limit);
    }

    public ProductsResponseDTO getProductsFresh(String tipo, int limit) {
        return buildProducts(tipo, limit);
    }

    private ProductsResponseDTO buildProducts(String tipo, int limit) {
        return runSafely("lista de produtos '" + tipo + "'", () -> {
            log.info("Buscando produtos tipo: {} (limit: {})", tipo, limit);

            List<Object[]> results;

            switch (tipo.toLowerCase()) {
                case "top":
                    results = financialRepository.getTopProducts(limit);
                    break;
                case "baixa-margem":
                    results = financialRepository.getLowMarginProducts(limit);
                    break;
                case "parados":
                    results = financialRepository.getStuckProducts(limit);
                    break;
                default:
                    throw new IllegalArgumentException("Tipo inválido: " + tipo + ". Use: top, baixa-margem ou parados");
            }

            List<ProductRankingDTO> products = new ArrayList<>();

            for (Object[] row : results) {
                if (row == null) continue;

                ProductRankingDTO dto = new ProductRankingDTO();

                if ("top".equalsIgnoreCase(tipo) && row.length >= 8) {
                    // Top produtos: [id, nome, categoria, preco, precoCusto, vezesVendido, receitaTotal, margem]
                    dto.setId(toLong(row[0]));
                    dto.setNome(toString(row[1]));
                    dto.setCategoria(toString(row[2]));
                    dto.setPreco(toLong(row[3]));
                    dto.setPrecoCusto(toLong(row[4]));
                    dto.setVezesVendido(toInteger(row[5]));
                    dto.setReceitaTotal(toLong(row[6]));
                    dto.setMargem(toDouble(row[7]));
                } else if (row.length >= 7) {
                    // Baixa margem / Parados: [id, nome, categoria, preco, precoCusto, margem, estoque]
                    dto.setId(toLong(row[0]));
                    dto.setNome(toString(row[1]));
                    dto.setCategoria(toString(row[2]));
                    dto.setPreco(toLong(row[3]));
                    dto.setPrecoCusto(toLong(row[4]));
                    dto.setMargem(toDouble(row[5]));
                    dto.setEstoque(toInteger(row[6]));
                }

                products.add(dto);
            }

            ProductsResponseDTO response = new ProductsResponseDTO();
            response.setTipo(tipo);
            response.setProdutos(products);

            return response;
        }, () -> emptyProductsResponse(tipo));
    }

    @Cacheable(value = "soldProductsByMonth", key = "#year + '_' + #month", unless = "#result == null")
    public SoldProductsByMonthResponseDTO getSoldProductsByMonth(int year, int month) {
        return buildSoldProductsByMonth(year, month);
    }

    public SoldProductsByMonthResponseDTO getSoldProductsByMonthFresh(int year, int month) {
        return buildSoldProductsByMonth(year, month);
    }

    public FinancialDiagnosticsDTO getDiagnostics() {
        return runSafely("diagnóstico financeiro", () -> {
            LocalDate now = LocalDate.now();
            int year = now.getYear();
            int month = now.getMonthValue();

            Object[] monthRaw = normalizeRow(financialRepository.getMonthMetricsRaw(year, month));
            Object[] stockRaw = normalizeRow(financialRepository.getStockMetricsRaw());

            FinancialDiagnosticsDTO dto = new FinancialDiagnosticsDTO();
            dto.setJdbcUrl(datasourceUrl);
            dto.setCurrentDatabase(financialRepository.getCurrentDatabase());
            dto.setRuntimeYear(year);
            dto.setRuntimeMonth(month);
            dto.setVendidosMes(monthRaw != null ? toInteger(monthRaw[0]) : 0);
            dto.setVendasMes(monthRaw != null ? toLong(monthRaw[1]) : 0L);
            dto.setCustoMes(monthRaw != null ? toLong(monthRaw[2]) : 0L);
            dto.setLucroMes(monthRaw != null ? toLong(monthRaw[3]) : 0L);
            dto.setEstoqueQtd(stockRaw != null ? toInteger(stockRaw[0]) : 0);
            dto.setEstoqueCusto(stockRaw != null ? toLong(stockRaw[1]) : 0L);
            dto.setEstoquePotencial(stockRaw != null ? toLong(stockRaw[2]) : 0L);
            return dto;
        }, this::emptyDiagnostics);
    }

    private SoldProductsByMonthResponseDTO buildSoldProductsByMonth(int year, int month) {
        return runSafely("vendidos por mês", () -> {
            List<Object[]> results = financialRepository.getSoldProductsByMonth(year, month);

            List<SoldProductDTO> soldProducts = new ArrayList<>();
            long totalValue = 0L;

            for (Object[] row : results) {
                if (row == null || row.length < 5) continue;

                SoldProductDTO dto = new SoldProductDTO();
                dto.setId(toLong(row[0]));
                dto.setNome(toString(row[1]));
                dto.setCategoria(toString(row[2]));
                dto.setPreco(toLong(row[3]));
                dto.setSoldDate(toString(row[4]));

                totalValue += dto.getPreco() != null ? dto.getPreco() : 0L;
                soldProducts.add(dto);
            }

            SoldProductsByMonthResponseDTO response = new SoldProductsByMonthResponseDTO();
            response.setAno(year);
            response.setMes(month);
            response.setQuantidadeVendidos(soldProducts.size());
            response.setValorTotalVendido(totalValue);
            response.setProdutos(soldProducts);
            return response;
        }, () -> emptySoldProductsByMonth(year, month));
    }

    /**
     * Formata chave do mês para exibição
     * "2026-02" -> "fevereiro de 2026"
     */
    private String formatMonthDisplay(String monthKey) {
        if (monthKey == null || monthKey.isEmpty()) {
            return "";
        }
        
        try {
            String[] parts = monthKey.split("-");
            if (parts.length != 2) return monthKey;
            
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            
            LocalDate date = LocalDate.of(year, month, 1);
            String monthName = date.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
            
            return monthName + " de " + year;
        } catch (Exception e) {
            log.warn("Erro ao formatar mês: {}", monthKey, e);
            return monthKey;
        }
    }

    // ==================== Métodos auxiliares de conversão ====================
    
    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof BigInteger) return ((BigInteger) value).longValue();
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Double) return ((Double) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Erro ao converter para Long: {}", value, e);
            return 0L;
        }
    }
    
    private Integer toInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof BigInteger) return ((BigInteger) value).intValue();
        if (value instanceof BigDecimal) return ((BigDecimal) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Erro ao converter para Integer: {}", value, e);
            return 0;
        }
    }
    
    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof BigDecimal) return ((BigDecimal) value).doubleValue();
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Erro ao converter para Double: {}", value, e);
            return 0.0;
        }
    }
    
    private String toString(Object value) {
        return value != null ? value.toString() : "";
    }

    private Object[] normalizeRow(Object[] row) {
        if (row == null) {
            return new Object[0];
        }
        if (row.length == 1 && row[0] instanceof Object[]) {
            return (Object[]) row[0];
        }
        return row;
    }

    private <T> T runSafely(String operation, Supplier<T> action, Supplier<T> fallback) {
        try {
            return action.get();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao carregar {}. Retornando fallback vazio.", operation, ex);
            return fallback.get();
        }
    }

    private CurrentMonthMetricsDTO emptyCurrentMonthMetrics() {
        return new CurrentMonthMetricsDTO(0L, 0L, 0L, 0.0, 0, 0L, "", 0.0);
    }

    private GeneralMetricsDTO emptyGeneralMetrics() {
        return new GeneralMetricsDTO(0L, 0L, 0, 0.0);
    }

    private StockMetricsDTO emptyStockMetrics() {
        return new StockMetricsDTO(0, 0L, 0L, 0L, 0);
    }

    private EvolutionResponseDTO emptyEvolutionResponse() {
        return new EvolutionResponseDTO(new ArrayList<>(), 1L);
    }

    private ProductsResponseDTO emptyProductsResponse(String tipo) {
        return new ProductsResponseDTO(tipo, new ArrayList<>());
    }

    private SoldProductsByMonthResponseDTO emptySoldProductsByMonth(int year, int month) {
        return new SoldProductsByMonthResponseDTO(year, month, 0, 0L, new ArrayList<>());
    }

    private FinancialDiagnosticsDTO emptyDiagnostics() {
        LocalDate now = LocalDate.now();
        return new FinancialDiagnosticsDTO(
            datasourceUrl,
            "",
            now.getYear(),
            now.getMonthValue(),
            0,
            0L,
            0L,
            0L,
            0,
            0L,
            0L
        );
    }
}
