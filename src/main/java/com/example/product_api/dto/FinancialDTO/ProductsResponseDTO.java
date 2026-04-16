package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductsResponseDTO {
    private String tipo;  // "top", "baixa-margem" ou "parados"
    private List<ProductRankingDTO> produtos;
}
