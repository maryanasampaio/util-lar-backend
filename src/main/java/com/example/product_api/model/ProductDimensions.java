package com.example.product_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDimensions {
    private Integer width;
    private Integer height;
    private Integer depth;
    private String unit;
}
