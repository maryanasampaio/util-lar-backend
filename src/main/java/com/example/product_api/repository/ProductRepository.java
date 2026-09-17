package com.example.product_api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.product_api.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);

        @Query(value = "SELECT "
                        + "p.id AS id, "
                        + "p.name AS name, "
                        + "p.slug AS slug, "
                        + "p.description AS description, "
                        + "p.price AS price, "
                        + "p.original_price AS originalPrice, "
                        + "p.cost_price AS costPrice, "
                        + "p.product_condition AS `condition`, "
                        + "p.category AS category, "
                        + "CASE "
                        + "WHEN p.images IS NULL OR TRIM(p.images) = '' THEN NULL "
                        + "WHEN JSON_VALID(p.images) THEN JSON_UNQUOTE(JSON_EXTRACT(p.images, '$[0]')) "
                        + "WHEN LOCATE(',', p.images) > 0 THEN TRIM(SUBSTRING_INDEX(p.images, ',', 1)) "
                        + "ELSE TRIM(p.images) "
                        + "END AS firstImage, "
                        + "p.stock AS stock, "
                        + "p.dimensions AS dimensionsJson, "
                        + "p.material AS material, "
                        + "p.color AS color, "
                        + "p.brand AS brand, "
                        + "p.warranty AS warranty, "
                        + "p.featured AS featured, "
                        + "p.sold_date AS soldDate, "
                        + "p.disponivel AS disponivel, "
                        + "p.created_at AS createdAt, "
                        + "p.updated_at AS updatedAt "
                        + "FROM tb_produto p "
                        + "WHERE (:category IS NULL OR :category = '' OR p.category = :category) "
                        + "AND (:conditionValue IS NULL OR :conditionValue = '' OR p.product_condition = :conditionValue) "
                        + "AND (:featured IS NULL OR p.featured = :featured) "
                        + "AND (:soldDateFilter IS NULL OR :soldDateFilter <> 'null' OR p.sold_date IS NULL) "
                        + "ORDER BY p.created_at DESC",
                countQuery = "SELECT COUNT(*) "
                                + "FROM tb_produto p "
                                + "WHERE (:category IS NULL OR :category = '' OR p.category = :category) "
                                + "AND (:conditionValue IS NULL OR :conditionValue = '' OR p.product_condition = :conditionValue) "
                                + "AND (:featured IS NULL OR p.featured = :featured) "
                                + "AND (:soldDateFilter IS NULL OR :soldDateFilter <> 'null' OR p.sold_date IS NULL)",
        nativeQuery = true)
    Page<ProductListProjection> findListPage(
            @Param("category") String category,
            @Param("conditionValue") String condition,
            @Param("featured") Boolean featured,
            @Param("soldDateFilter") String soldDateFilter,
            Pageable pageable);

    @Query(value = "SELECT "
            + "p.id AS id, "
            + "p.name AS name, "
            + "p.slug AS slug, "
            + "p.description AS description, "
            + "p.price AS price, "
            + "p.original_price AS originalPrice, "
            + "p.cost_price AS costPrice, "
            + "p.product_condition AS `condition`, "
            + "p.category AS category, "
            + "p.images AS imagesJson, "
            + "p.stock AS stock, "
            + "p.dimensions AS dimensionsJson, "
            + "p.material AS material, "
            + "p.color AS color, "
            + "p.brand AS brand, "
            + "p.warranty AS warranty, "
            + "p.featured AS featured, "
            + "p.sold_date AS soldDate, "
            + "p.disponivel AS disponivel, "
            + "p.created_at AS createdAt, "
            + "p.updated_at AS updatedAt "
            + "FROM tb_produto p "
            + "WHERE p.id = :id "
            + "LIMIT 1", nativeQuery = true)
    Optional<ProductDetailProjection> findDetailById(@Param("id") Long id);
}
