package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneralMetricsDTO {
    private Long lucroTotal;             // centavos
    private Long vendasTotais;           // centavos
    private Integer itensVendidosTotal;  // unidades
    private Double margemMedia;          // percentual
}
