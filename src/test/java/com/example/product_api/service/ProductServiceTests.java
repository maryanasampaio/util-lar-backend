package com.example.product_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.product_api.dto.ProductDTO.ProductRequestDTO;
import com.example.product_api.dto.ProductDTO.ProductResponseDTO;
import com.example.product_api.mapper.ProductMapper;
import com.example.product_api.model.Product;
import com.example.product_api.repository.ProductDetailProjection;
import com.example.product_api.repository.ProductListProjection;
import com.example.product_api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductNotificationService productNotificationService;

    @Mock
    private StorageService storageService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        lenient().when(storageService.isConfigured()).thenReturn(true);
        ProductImageService imageService = new ProductImageService(storageService, "http://localhost:8080");
        productService = new ProductService(productRepository, new ProductMapper(imageService), productNotificationService, imageService);
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
    void shouldDeduplicateConcurrentCreateRequestsWithSamePayload() throws Exception {
        Set<Thread> threadsInsideCreate = ConcurrentHashMap.newKeySet();
        ProductRequestDTO request = updateRequest(159900L);
        request.setImages(new ThreadTrackingImages(request.getImages(), threadsInsideCreate));

        CountDownLatch saveStarted = new CountDownLatch(1);
        AtomicInteger saveCalls = new AtomicInteger();

        when(productRepository.existsBySlug("mesa-de-jantar")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            saveCalls.incrementAndGet();
            Product product = invocation.getArgument(0);
            saveStarted.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (threadsInsideCreate.size() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(2, threadsInsideCreate.size(), "a segunda requisicao nao entrou em create()");
            product.setId(1L);
            return product;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ProductResponseDTO> first = executor.submit(() -> productService.create(request));
            assertTrue(saveStarted.await(5, TimeUnit.SECONDS));

            Future<ProductResponseDTO> second = executor.submit(() -> productService.create(request));

            ProductResponseDTO firstResponse = first.get(10, TimeUnit.SECONDS);
            ProductResponseDTO secondResponse = second.get(2, TimeUnit.SECONDS);

            assertEquals(firstResponse.getId(), secondResponse.getId());
            assertEquals(1, saveCalls.get());
            verify(productNotificationService, times(1)).notifyNewProduct(any(Product.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldNotLeaveProductInDatabaseWhenImageUploadFails() {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Produto com foto");
        request.setImages(List.of("data:image/png;base64,iVBORw0KGgo="));

        when(productRepository.existsBySlug("produto-com-foto")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(77L);
            return product;
        });
        when(storageService.upload(any(), any(), any()))
                .thenThrow(new IllegalStateException("S3 indisponivel"));

        assertThrows(IllegalStateException.class, () -> productService.create(request));

        verify(productRepository).deleteById(77L);
        verify(productNotificationService, never()).notifyNewProduct(any(Product.class));
    }

    @Test
    void shouldReturnOnlyFirstImageOnListing() {
        String firstImage = "data:image/png;base64," + "A".repeat(20000);
        ProductListProjection existing = projection(1L, firstImage, "{\"width\":220,\"height\":95,\"depth\":105,\"unit\":\"cm\"}");

        when(productRepository.findListPage(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existing), PageRequest.of(0, 100), 1));

        List<ProductResponseDTO> response = productService.findAll(null, null, null, null, 0, 100);

        assertEquals(1, response.size());
        assertEquals(List.of(firstImage), response.get(0).getImages());
        assertEquals(220, response.get(0).getDimensions().getWidth());
    }

    @Test
    void shouldReturnOnlyRequestedPageOnListing() {
        ProductListProjection first = projection(1L, "https://cdn.exemplo.com/mesa.jpg", null);

        Pageable expectedPageable = PageRequest.of(0, 1);

        when(productRepository.findListPage(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), eq(expectedPageable)))
            .thenReturn(new PageImpl<>(List.of(first), expectedPageable, 2));

        List<ProductResponseDTO> response = productService.findAll(null, null, null, null, 0, 1);

        assertEquals(1, response.size());
        assertEquals(first.getId(), response.get(0).getId());
        verify(productRepository).findListPage(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), eq(expectedPageable));
    }

    @Test
    void shouldReturnAllImagesOnFindById() {
        ProductDetailProjection detail = detailProjection(
                1L,
                "[\"https://cdn.exemplo.com/mesa-capa.jpg\",\"https://cdn.exemplo.com/mesa-lateral.jpg\",\"https://cdn.exemplo.com/mesa-detalhe.jpg\"]",
                "{\"width\":220,\"height\":95,\"depth\":105,\"unit\":\"cm\"}"
        );

        when(productRepository.findDetailById(1L)).thenReturn(Optional.of(detail));

        ProductResponseDTO response = productService.findById(1L);

        assertEquals(3, response.getImages().size());
        assertEquals("https://cdn.exemplo.com/mesa-capa.jpg", response.getImages().get(0));
        assertEquals("https://cdn.exemplo.com/mesa-lateral.jpg", response.getImages().get(1));
        assertEquals("https://cdn.exemplo.com/mesa-detalhe.jpg", response.getImages().get(2));
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

    private static final class ThreadTrackingImages extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        private final transient Set<Thread> threads;

        ThreadTrackingImages(Collection<String> source, Set<Thread> threads) {
            super(source);
            this.threads = threads;
        }

        @Override
        public int hashCode() {
            threads.add(Thread.currentThread());
            return super.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other);
        }
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

    private ProductListProjection projection(Long id, String firstImage, String dimensionsJson) {
        ProductListProjection projection = org.mockito.Mockito.mock(ProductListProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getName()).thenReturn("Mesa de jantar");
        when(projection.getSlug()).thenReturn("mesa-de-jantar");
        when(projection.getDescription()).thenReturn("Mesa em madeira macica");
        when(projection.getPrice()).thenReturn(129900L);
        when(projection.getOriginalPrice()).thenReturn(159900L);
        when(projection.getCostPrice()).thenReturn(99900L);
        when(projection.getCondition()).thenReturn("novo");
        when(projection.getCategory()).thenReturn("Mesas");
        when(projection.getFirstImage()).thenReturn(firstImage);
        when(projection.getStock()).thenReturn(2);
        when(projection.getDimensionsJson()).thenReturn(dimensionsJson);
        when(projection.getMaterial()).thenReturn("Madeira");
        when(projection.getColor()).thenReturn("Castanho");
        when(projection.getBrand()).thenReturn("Util Lar");
        when(projection.getWarranty()).thenReturn("12 meses");
        when(projection.getFeatured()).thenReturn(Boolean.TRUE);
        when(projection.getDisponivel()).thenReturn(1);
        return projection;
    }

    private ProductDetailProjection detailProjection(Long id, String imagesJson, String dimensionsJson) {
        ProductDetailProjection projection = org.mockito.Mockito.mock(ProductDetailProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getName()).thenReturn("Mesa de jantar");
        when(projection.getSlug()).thenReturn("mesa-de-jantar");
        when(projection.getDescription()).thenReturn("Mesa em madeira macica");
        when(projection.getPrice()).thenReturn(129900L);
        when(projection.getOriginalPrice()).thenReturn(159900L);
        when(projection.getCostPrice()).thenReturn(99900L);
        when(projection.getCondition()).thenReturn("novo");
        when(projection.getCategory()).thenReturn("Mesas");
        when(projection.getImagesJson()).thenReturn(imagesJson);
        when(projection.getStock()).thenReturn(2);
        when(projection.getDimensionsJson()).thenReturn(dimensionsJson);
        when(projection.getMaterial()).thenReturn("Madeira");
        when(projection.getColor()).thenReturn("Castanho");
        when(projection.getBrand()).thenReturn("Util Lar");
        when(projection.getWarranty()).thenReturn("12 meses");
        when(projection.getFeatured()).thenReturn(Boolean.TRUE);
        when(projection.getDisponivel()).thenReturn(1);
        return projection;
    }
}