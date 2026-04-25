package com.example.product_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
    void shouldCreateProductWithDefaultValuesWhenFieldsAreMissing() {
        ProductRequestDTO request = new ProductRequestDTO();

        when(productRepository.existsBySlug("produto-sem-nome")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            return product;
        });

        ProductResponseDTO response = productService.create(request);

        assertEquals("Produto sem nome", response.getName());
        assertEquals("produto-sem-nome", response.getSlug());
        assertEquals(0L, response.getPrice());
        assertEquals(null, response.getOriginalPrice());
        assertEquals("novo", response.getCondition());
        assertEquals("Sem categoria", response.getCategory());
        assertEquals(0, response.getStock());
        assertEquals(Boolean.FALSE, response.getFeatured());
        verify(productNotificationService).notifyNewProduct(any(Product.class));
    }

    @Test
    void shouldAllowRepeatedProductNameOnCreateByGeneratingUniqueSlug() {
        ProductRequestDTO request = updateRequest(159900L);

        when(productRepository.existsBySlug("mesa-de-jantar")).thenReturn(true);
        when(productRepository.existsBySlug("mesa-de-jantar-2")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(2L);
            return product;
        });

        ProductResponseDTO response = productService.create(request);

        assertEquals("Mesa de jantar", response.getName());
        assertEquals("mesa-de-jantar-2", response.getSlug());
        verify(productNotificationService).notifyNewProduct(any(Product.class));
    }

    @Test
    void shouldOmitLargeInlineImagesFromListing() {
        Product existing = existingProduct();
        existing.setImages(List.of("data:image/png;base64," + "A".repeat(20000), "https://cdn.exemplo.com/mesa.jpg"));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existing), PageRequest.of(0, 100), 1));

        List<ProductResponseDTO> response = productService.findAll(null, null, null, null);

        assertEquals(1, response.size());
        assertEquals(List.of("https://cdn.exemplo.com/mesa.jpg"), response.get(0).getImages());
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

    @Test
    void shouldPreserveExistingValuesWhenUpdatingWithEmptyPayload() {
        Product existing = existingProduct();
        ProductRequestDTO request = new ProductRequestDTO();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO response = productService.update(1L, request);

        assertNotNull(response);
        assertEquals("Mesa de jantar", response.getName());
        assertEquals("mesa-de-jantar", response.getSlug());
        assertEquals(129900L, response.getPrice());
        assertEquals(159900L, response.getOriginalPrice());
        assertEquals("Mesas", response.getCategory());
        verify(productNotificationService, never()).notifyPriceUpdated(any(Product.class), any(Long.class));
    }

    @Test
    void shouldAllowClearingOriginalPriceOnUpdate() {
        Product existing = existingProduct();
        ProductRequestDTO request = new ProductRequestDTO();
        request.setOriginalPrice(null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO response = productService.update(1L, request);

        assertEquals(null, response.getOriginalPrice());
        verify(productNotificationService, never()).notifyPriceUpdated(any(Product.class), any(Long.class));
    }

    @Test
    void shouldAllowRepeatedProductNameOnUpdateByGeneratingUniqueSlug() {
        Product existing = existingProduct();
        ProductRequestDTO request = updateRequest(129900L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySlugAndIdNot("mesa-de-jantar", 1L)).thenReturn(true);
        when(productRepository.existsBySlugAndIdNot("mesa-de-jantar-2", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO response = productService.update(1L, request);

        assertEquals("Mesa de jantar", response.getName());
        assertEquals("mesa-de-jantar-2", response.getSlug());
        verify(productNotificationService, never()).notifyPriceUpdated(any(Product.class), any(Long.class));
    }

    private Product existingProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Mesa de jantar");
        product.setSlug("mesa-de-jantar");
        product.setDescription("Mesa em madeira macica");
        product.setPrice(129900L);
        product.setOriginalPrice(159900L);
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
            159900L,
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