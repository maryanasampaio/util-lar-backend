package com.example.product_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.product_api.model.CardBrand;

public interface CardBrandRepository extends JpaRepository<CardBrand, String> {
    List<CardBrand> findByActiveTrueOrderByNameAsc();
    boolean existsByIdAndActiveTrue(String id);
}
