
package com.example.product_api.dto.ProductDTO;

import java.time.Instant;
import java.util.List;

import com.example.product_api.model.ProductDimensions;
import com.example.product_api.model.Product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long price;
    private Long costPrice;
    private String condition;
    private String category;
    private List<String> images;
    private Integer stock;
    private ProductDimensions dimensions;
    private String material;
    private String color;
    private String brand;
    private String warranty;
    private Boolean featured;
    private Instant soldDate;
    private Integer disponivel;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponseDTO from(Product p) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setSlug(p.getSlug());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setCostPrice(p.getCostPrice());
        dto.setCondition(p.getCondition());
        dto.setCategory(p.getCategory());
        dto.setImages(p.getImages());
        dto.setStock(p.getStock());
        dto.setDimensions(p.getDimensions());
        dto.setMaterial(p.getMaterial());
        dto.setColor(p.getColor());
        dto.setBrand(p.getBrand());
        dto.setWarranty(p.getWarranty());
        dto.setFeatured(p.getFeatured());
        dto.setSoldDate(p.getSoldDate());
        dto.setDisponivel(p.getDisponivel());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
