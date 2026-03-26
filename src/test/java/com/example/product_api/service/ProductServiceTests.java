package com.example.product_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.product_api.dto.ProductDTO.ProductRequestDTO;
import com.example.product_api.dto.ProductDTO.ProductResponseDTO;
import com.example.product_api.mapper.ProductMapper;
import com.example.product_api.model.Product;
import com.example.product_api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductNotificationService productNotificationService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, new ProductMapper(), productNotificationService);
    }

    @Test
    void shouldNotifyWhenPriceChangesOnUpdate() {
        Product existing = existingProduct();
        ProductRequestDTO request = updateRequest(159900L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySlugAndIdNot("mesa-de-jantar", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO response = productService.update(1L, request);

        assertEquals(159900L, response.getPrice());
        verify(productNotificationService).notifyPriceUpdated(any(Product.class), org.mockito.ArgumentMatchers.eq(129900L));
    }

    @Test
    void shouldNotNotifyWhenPriceDoesNotChangeOnUpdate() {
        Product existing = existingProduct();
        ProductRequestDTO request = updateRequest(129900L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySlugAndIdNot("mesa-de-jantar", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO response = productService.update(1L, request);

        assertEquals(129900L, response.getPrice());
        verify(productNotificationService, never()).notifyPriceUpdated(any(Product.class), any(Long.class));
    }

    private Product existingProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Mesa de jantar");
        product.setSlug("mesa-de-jantar");
        product.setDescription("Mesa em madeira macica");
        product.setPrice(129900L);
        product.setCostPrice(99900L);
        product.setCondition("novo");
        product.setCategory("Mesas");
        product.setImages(List.of("https://cdn.exemplo.com/mesa.jpg"));
        product.setStock(2);
        product.setMaterial("Madeira");
        product.setColor("Castanho");
        product.setBrand("Util Lar");
        product.setWarranty("12 meses");
        product.setFeatured(Boolean.TRUE);
        product.setDisponivel(1);
        return product;
    }

    private ProductRequestDTO updateRequest(Long price) {
        return new ProductRequestDTO(
                "Mesa de jantar",
                "Mesa em madeira macica",
                price,
                99900L,
                "novo",
                "Mesas",
                List.of("https://cdn.exemplo.com/mesa.jpg"),
                2,
                null,
                "Madeira",
                "Castanho",
                "Util Lar",
                "12 meses",
                Boolean.TRUE
        );
    }
}