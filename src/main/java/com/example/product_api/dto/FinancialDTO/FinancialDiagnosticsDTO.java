package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinancialDiagnosticsDTO {
    private String jdbcUrl;
    private String currentDatabase;
    private Integer runtimeYear;
    private Integer runtimeMonth;
    private Integer vendidosMes;
    private Long vendasMes;
    private Long custoMes;
    private Long lucroMes;
    private Integer estoqueQtd;
    private Long estoqueCusto;
    private Long estoquePotencial;
}
