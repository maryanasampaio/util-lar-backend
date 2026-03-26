package com.example.product_api.dto.ProductDTO;

import java.util.List;

import com.example.product_api.model.ProductDimensions;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO {

    @Size(max = 200, message = "Name deve ter no maximo 200 caracteres")
    private String name;

    private String description;

    @PositiveOrZero(message = "Price deve ser inteiro positivo em centavos")
    private Long price; // centavos

    @PositiveOrZero(message = "CostPrice deve ser inteiro positivo em centavos")
    private Long costPrice;

    @Pattern(regexp = "^$|novo|seminovo", message = "Condition deve ser 'novo' ou 'seminovo'")
    private String condition;

    @Size(max = 100, message = "Category deve ter no maximo 100 caracteres")
    private String category;

    private List<String> images;

    @PositiveOrZero(message = "Stock não pode ser negativo")
    private Integer stock;

    private ProductDimensions dimensions;
    private String material;
    private String color;
    private String brand;
    private String warranty;

    private Boolean featured;
}
