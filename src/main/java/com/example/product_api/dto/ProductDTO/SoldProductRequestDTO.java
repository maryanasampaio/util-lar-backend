package com.example.product_api.dto.ProductDTO;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoldProductRequestDTO {
    private Instant soldDate;
}
