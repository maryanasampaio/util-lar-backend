package com.example.product_api.service;

import com.example.product_api.dto.FinancialDTO.DashboardResponseDTO;
import com.example.product_api.dto.FinancialDTO.EvolutionResponseDTO;
import com.example.product_api.dto.FinancialDTO.ProductsResponseDTO;
import com.example.product_api.repository.FinancialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTests {

    @Mock
    private FinancialRepository financialRepository;

    @InjectMocks
    private FinancialService financialService;

    @Test
    void shouldReturnEmptyDashboardWhenFinancialQueriesFail() {
        when(financialRepository.getMonthMetrics(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
            .thenThrow(new RuntimeException("db offline"));
        when(financialRepository.getGeneralMetrics())
            .thenThrow(new RuntimeException("db offline"));
        when(financialRepository.getStockMetrics())
            .thenThrow(new RuntimeException("db offline"));

        DashboardResponseDTO response = financialService.getDashboardFresh();

        assertNotNull(response);
        assertNotNull(response.getMesAtual());
        assertNotNull(response.getGeral());
        assertNotNull(response.getEstoque());
        assertEquals(0L, response.getMesAtual().getLucro());
        assertEquals(0L, response.getGeral().getVendasTotais());
        assertEquals(0, response.getEstoque().getQuantidade());
    }

    @Test
    void shouldReturnEmptyEvolutionWhenRepositoryFails() {
        when(financialRepository.getMonthlyEvolution(6))
            .thenThrow(new RuntimeException("query failed"));

        EvolutionResponseDTO response = financialService.getMonthlyEvolutionFresh(6);

        assertNotNull(response);
        assertNotNull(response.getMeses());
        assertEquals(0, response.getMeses().size());
        assertEquals(1L, response.getLucroMaximoMes());
    }

    @Test
    void shouldReturnEmptyProductsListWhenRepositoryFails() {
        when(financialRepository.getTopProducts(5))
            .thenThrow(new RuntimeException("query failed"));

        ProductsResponseDTO response = financialService.getProductsFresh("top", 5);

        assertNotNull(response);
        assertEquals("top", response.getTipo());
        assertNotNull(response.getProdutos());
        assertEquals(0, response.getProdutos().size());
    }
}