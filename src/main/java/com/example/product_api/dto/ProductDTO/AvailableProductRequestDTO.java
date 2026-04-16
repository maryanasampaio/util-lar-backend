package com.example.product_api.dto.ProductDTO;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableProductRequestDTO {
    @Positive(message = "Stock deve ser maior que zero")
    private Integer stock;
}
