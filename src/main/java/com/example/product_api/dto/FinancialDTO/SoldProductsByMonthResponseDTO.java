package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoldProductsByMonthResponseDTO {
    private Integer ano;
    private Integer mes;
    private Integer quantidadeVendidos;
    private Long valorTotalVendido;
    private List<SoldProductDTO> produtos;
}
