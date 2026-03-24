package com.example.product_api.dto.ProductDTO;

import java.util.List;

import com.example.product_api.model.ProductDimensions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Name é obrigatório")
    @Size(min = 3, max = 200, message = "Name deve ter entre 3 e 200 caracteres")
    private String name;

    private String description;

    @NotNull(message = "Price é obrigatório")
    @PositiveOrZero(message = "Price deve ser inteiro positivo em centavos")
    private Long price; // centavos

    @PositiveOrZero(message = "CostPrice deve ser inteiro positivo em centavos")
    private Long costPrice;

    @NotBlank(message = "Condition é obrigatório")
    @Pattern(regexp = "novo|seminovo", message = "Condition deve ser 'novo' ou 'seminovo'")
    private String condition;

    @NotBlank(message = "Category é obrigatório")
    @Size(min = 2, max = 100, message = "Category deve ter entre 2 e 100 caracteres")
    private String category;

    private List<String> images;

    @NotNull(message = "Stock é obrigatório")
    @PositiveOrZero(message = "Stock não pode ser negativo")
    private Integer stock;

    private ProductDimensions dimensions;
    private String material;
    private String color;
    private String brand;
    private String warranty;

    @NotNull(message = "Featured é obrigatório")
    private Boolean featured;
}
