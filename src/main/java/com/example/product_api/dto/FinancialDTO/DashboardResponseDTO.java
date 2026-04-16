package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponseDTO {
    private CurrentMonthMetricsDTO mesAtual;
    private GeneralMetricsDTO geral;
    private StockMetricsDTO estoque;
}
