package com.example.product_api.dto.ProductDTO;

import java.util.List;

import com.example.product_api.model.ProductDimensions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductRequestDTO {

    @Size(max = 200, message = "Name deve ter no maximo 200 caracteres")
    private String name;

    private String description;

    @PositiveOrZero(message = "Price deve ser inteiro positivo em centavos")
    private Long price; // centavos

    @PositiveOrZero(message = "OriginalPrice deve ser inteiro positivo em centavos")
    private Long originalPrice;

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

    @JsonIgnore
    private boolean originalPriceDefined;

    public ProductRequestDTO(
            String name,
            String description,
            Long price,
            Long originalPrice,
            Long costPrice,
            String condition,
            String category,
            List<String> images,
            Integer stock,
            ProductDimensions dimensions,
            String material,
            String color,
            String brand,
            String warranty,
            Boolean featured) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.originalPriceDefined = true;
        this.costPrice = costPrice;
        this.condition = condition;
        this.category = category;
        this.images = images;
        this.stock = stock;
        this.dimensions = dimensions;
        this.material = material;
        this.color = color;
        this.brand = brand;
        this.warranty = warranty;
        this.featured = featured;
    }

    @JsonSetter("originalPrice")
    public void setOriginalPrice(Long originalPrice) {
        this.originalPrice = originalPrice;
        this.originalPriceDefined = true;
    }
}
