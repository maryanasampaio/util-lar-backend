package com.example.product_api.controller;

import com.example.product_api.dto.FinancialDTO.DashboardResponseDTO;
import com.example.product_api.dto.FinancialDTO.EvolutionResponseDTO;
import com.example.product_api.dto.FinancialDTO.FinancialDiagnosticsDTO;
import com.example.product_api.dto.FinancialDTO.ProductsResponseDTO;
import com.example.product_api.dto.FinancialDTO.SoldProductsByMonthResponseDTO;
import com.example.product_api.service.FinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller para endpoints financeiros.
 * 
 * ⚠️ IMPORTANTE: Todos os endpoints deste controller requerem autenticação JWT.
 * É necessário enviar o header: Authorization: Bearer {token}
 * Apenas usuários autenticados (admin) podem acessar dados financeiros.
 */
@RestController
@RequestMapping({"/api/financeiro", "/financeiro"})
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    /**
     * GET /api/financeiro/dashboard
     * 
     * Retorna consolidado de métricas:
     * - Mês atual (lucro, vendas, custo, margem, itens vendidos, ticket médio, produto mais vendido, variação)
     * - Métricas gerais (lucro total, vendas totais, itens vendidos total, margem média)
     * - Estoque (quantidade, valor custo, valor potencial, lucro potencial, produtos parados)
     * 
     * 🔒 Requer autenticação JWT (admin)
     * Cache: 5 minutos
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDTO> getDashboard(
        @RequestParam(defaultValue = "false") boolean refresh
    ) {
        return ResponseEntity.ok(refresh ? financialService.getDashboardFresh() : financialService.getDashboard());
    }

    /**
     * GET /api/financeiro/evolucao?meses=6
     * 
     * Retorna evolução mensal dos últimos N meses (padrão: 6)
     * 
     * @param meses Número de meses a retornar (padrão: 6)
     * @return Lista de métricas mensais + lucro máximo do período
     * 
     * 🔒 Requer autenticação JWT (admin)
     * Cache: 5 minutos
     */
    @GetMapping("/evolucao")
    public ResponseEntity<EvolutionResponseDTO> getMonthlyEvolution(
        @RequestParam(defaultValue = "6") int meses,
        @RequestParam(defaultValue = "false") boolean refresh
    ) {
        // Validar parâmetro
        if (meses < 1) meses = 6;
        if (meses > 24) meses = 24; // Máximo 2 anos
        
        return ResponseEntity.ok(refresh
            ? financialService.getMonthlyEvolutionFresh(meses)
            : financialService.getMonthlyEvolution(meses));
    }

    /**
     * GET /api/financeiro/produtos?tipo=top&limit=3
     * 
     * Retorna produtos por tipo (top, baixa-margem, parados)
     * 
     * @param tipo Tipo de produto: "top" (mais vendidos), "baixa-margem" (menor margem), "parados" (estoque baixo)
     * @param limit Quantidade de produtos a retornar (padrão: 5, máximo: 20)
     * @return Lista de produtos do tipo especificado
     * 
     * 🔒 Requer autenticação JWT (admin)
     * Cache: 10 minutos
     */
    @GetMapping("/produtos")
    public ResponseEntity<ProductsResponseDTO> getProducts(
        @RequestParam(defaultValue = "top") String tipo,
        @RequestParam(defaultValue = "5") int limit,
        @RequestParam(defaultValue = "false") boolean refresh
    ) {
        // Validar parâmetros
        if (limit < 1) limit = 5;
        if (limit > 20) limit = 20;
        
        tipo = tipo.toLowerCase().trim();
        if (!tipo.equals("top") && !tipo.equals("baixa-margem") && !tipo.equals("parados")) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Tipo inválido. Use: top, baixa-margem ou parados"));
        }
        
        return ResponseEntity.ok(refresh
            ? financialService.getProductsFresh(tipo, limit)
            : financialService.getProducts(tipo, limit));
    }

    /**
     * GET /api/financeiro/vendidos?ano=2026&mes=3
     *
     * Lista todos os produtos vendidos no mês informado.
     *
     * @param ano Ano da consulta (padrão: ano atual)
     * @param mes Mês da consulta 1..12 (padrão: mês atual)
     * @param refresh true para ignorar cache
     */
    @GetMapping("/vendidos")
    public ResponseEntity<SoldProductsByMonthResponseDTO> getSoldProductsByMonth(
        @RequestParam(required = false) Integer ano,
        @RequestParam(required = false) Integer mes,
        @RequestParam(defaultValue = "false") boolean refresh
    ) {
        LocalDate now = LocalDate.now();
        int year = ano != null ? ano : now.getYear();
        int month = mes != null ? mes : now.getMonthValue();

        if (month < 1 || month > 12) {
            month = now.getMonthValue();
        }

        return ResponseEntity.ok(refresh
            ? financialService.getSoldProductsByMonthFresh(year, month)
            : financialService.getSoldProductsByMonth(year, month));
    }

    /**
     * Cria resposta de erro para tipo inválido
     */
    private ProductsResponseDTO createErrorResponse(String message) {
        ProductsResponseDTO error = new ProductsResponseDTO();
        error.setTipo("error");
        error.setProdutos(new java.util.ArrayList<>());
        return error;
    }

    @GetMapping("/diagnostico")
    public ResponseEntity<FinancialDiagnosticsDTO> getDiagnostics() {
        return ResponseEntity.ok(financialService.getDiagnostics());
    }
}
