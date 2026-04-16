package com.example.product_api.mapper;

import org.springframework.stereotype.Component;

import com.example.product_api.dto.ProductDTO.ProductRequestDTO;
import com.example.product_api.dto.ProductDTO.ProductResponseDTO;
import com.example.product_api.model.Product;

@Component
public class ProductMapper {
    private static final String DEFAULT_NAME = "Produto sem nome";
    private static final String DEFAULT_DESCRIPTION = "";
    private static final String DEFAULT_CONDITION = "novo";
    private static final String DEFAULT_CATEGORY = "Sem categoria";

    public ProductResponseDTO toResponse(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSlug(product.getSlug());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCostPrice(product.getCostPrice());
        dto.setCondition(product.getCondition());
        dto.setCategory(product.getCategory());
        dto.setImages(product.getImages());
        dto.setStock(product.getStock());
        dto.setDimensions(product.getDimensions());
        dto.setMaterial(product.getMaterial());
        dto.setColor(product.getColor());
        dto.setBrand(product.getBrand());
        dto.setWarranty(product.getWarranty());
        dto.setFeatured(product.getFeatured());
        dto.setSoldDate(product.getSoldDate());
        dto.setDisponivel(product.getDisponivel());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }

    public void populateEntityForCreate(ProductRequestDTO dto, Product product) {
        product.setName(defaultIfBlank(dto != null ? dto.getName() : null, DEFAULT_NAME));
        product.setDescription(dto != null && dto.getDescription() != null ? dto.getDescription() : DEFAULT_DESCRIPTION);
        product.setPrice(dto != null && dto.getPrice() != null ? dto.getPrice() : 0L);
        product.setCostPrice(dto != null ? dto.getCostPrice() : null);
        product.setCondition(defaultIfBlank(dto != null ? dto.getCondition() : null, DEFAULT_CONDITION));
        product.setCategory(defaultIfBlank(dto != null ? dto.getCategory() : null, DEFAULT_CATEGORY));
        product.setImages(dto != null ? dto.getImages() : null);
        product.setStock(dto != null && dto.getStock() != null ? dto.getStock() : 0);
        product.setDimensions(dto != null ? dto.getDimensions() : null);
        product.setMaterial(dto != null ? dto.getMaterial() : null);
        product.setColor(dto != null ? dto.getColor() : null);
        product.setBrand(dto != null ? dto.getBrand() : null);
        product.setWarranty(dto != null ? dto.getWarranty() : null);
        product.setFeatured(dto != null && dto.getFeatured() != null ? dto.getFeatured() : Boolean.FALSE);
    }

    public void applyPartialUpdate(ProductRequestDTO dto, Product product) {
        if (dto == null) {
            return;
        }

        if (hasText(dto.getName())) {
            product.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
        if (dto.getCostPrice() != null) {
            product.setCostPrice(dto.getCostPrice());
        }
        if (hasText(dto.getCondition())) {
            product.setCondition(dto.getCondition().trim());
        }
        if (hasText(dto.getCategory())) {
            product.setCategory(dto.getCategory().trim());
        }
        if (dto.getImages() != null) {
            product.setImages(dto.getImages());
        }
        if (dto.getStock() != null) {
            product.setStock(dto.getStock());
        }
        if (dto.getDimensions() != null) {
            product.setDimensions(dto.getDimensions());
        }
        if (dto.getMaterial() != null) {
            product.setMaterial(dto.getMaterial());
        }
        if (dto.getColor() != null) {
            product.setColor(dto.getColor());
        }
        if (dto.getBrand() != null) {
            product.setBrand(dto.getBrand());
        }
        if (dto.getWarranty() != null) {
            product.setWarranty(dto.getWarranty());
        }
        if (dto.getFeatured() != null) {
            product.setFeatured(dto.getFeatured());
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
