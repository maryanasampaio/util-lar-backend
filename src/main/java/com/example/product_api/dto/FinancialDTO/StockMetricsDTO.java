package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockMetricsDTO {
    private Integer quantidade;          // unidades em estoque
    private Long valorCusto;             // centavos
    private Long valorPotencial;         // centavos
    private Long lucroPotencial;         // centavos
    private Integer produtosParados;     // produtos com stock < 2
}
