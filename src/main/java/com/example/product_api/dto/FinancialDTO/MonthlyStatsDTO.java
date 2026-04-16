package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyStatsDTO {
    private String mes;              // "fevereiro de 2026"
    private String mesChave;         // "2026-02"
    private Long vendas;             // centavos
    private Long custo;              // centavos
    private Long lucro;              // centavos
    private Integer itensVendidos;   // unidades
    private Double margem;           // percentual
}
