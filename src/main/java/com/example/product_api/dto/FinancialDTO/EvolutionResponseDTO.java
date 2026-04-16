package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvolutionResponseDTO {
    private List<MonthlyStatsDTO> meses;
    private Long lucroMaximoMes;  // maior lucro mensal (para gráficos)
}
