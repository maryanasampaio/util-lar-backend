package com.example.product_api.model;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.example.product_api.util.DimensionsJsonConverter;
import com.example.product_api.util.StringListJsonConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_produto")
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private Long price;

    @Column(name = "cost_price")
    private Long costPrice;

    @Column(name = "product_condition", nullable = false, length = 10)
    private String condition;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(columnDefinition = "LONGTEXT")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> images;

    @Column(nullable = false)
    private Integer stock;

    @Column(columnDefinition = "json")
    @Convert(converter = DimensionsJsonConverter.class)
    private ProductDimensions dimensions;

    @Column(length = 100)
    private String material;

    @Column(length = 50)
    private String color;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String warranty;

    @Column(nullable = false)
    private Boolean featured;

    @Column(name = "sold_date")
    private Instant soldDate;

    @Column(nullable = false)
    private Integer disponivel;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.stock == null) {
            this.stock = 0;
        }
        if (this.featured == null) {
            this.featured = Boolean.FALSE;
        }
        if (this.disponivel == null) {
            this.disponivel = 1;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

}
