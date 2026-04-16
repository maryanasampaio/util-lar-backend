package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoldProductDTO {
    private Long id;
    private String nome;
    private String categoria;
    private Long preco;
    private String soldDate;
}
