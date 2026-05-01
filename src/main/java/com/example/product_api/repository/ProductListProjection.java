package com.example.product_api.repository;

import java.time.Instant;

public interface ProductListProjection {
    Long getId();
    String getName();
    String getSlug();
    String getDescription();
    Long getPrice();
    Long getOriginalPrice();
    Long getCostPrice();
    String getCondition();
    String getCategory();
    String getFirstImage();
    Integer getStock();
    String getDimensionsJson();
    String getMaterial();
    String getColor();
    String getBrand();
    String getWarranty();
    Boolean getFeatured();
    Instant getSoldDate();
    Integer getDisponivel();
    Instant getCreatedAt();
    Instant getUpdatedAt();
}