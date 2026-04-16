package com.example.product_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.product_api.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
	boolean existsBySlug(String slug);
	boolean existsBySlugAndIdNot(String slug, Long id);
}
