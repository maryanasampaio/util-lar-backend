package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentMonthMetricsDTO {
    private Long lucro;                  // centavos
    private Long vendas;                 // centavos
    private Long custo;                  // centavos
    private Double margem;               // percentual
    private Integer itensVendidos;       // unidades
    private Long ticketMedio;            // centavos
    private String produtoMaisVendido;   // nome do produto
    private Double variacaoLucro;        // percentual vs mês anterior
}
