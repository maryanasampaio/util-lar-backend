package com.example.product_api.util;

import com.example.product_api.model.ProductDimensions;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DimensionsJsonConverter implements AttributeConverter<ProductDimensions, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ProductDimensions attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao serializar dimensions", e);
        }
    }

    @Override
    public ProductDimensions convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, ProductDimensions.class);
        } catch (Exception e) {
            return null;
        }
    }
}
