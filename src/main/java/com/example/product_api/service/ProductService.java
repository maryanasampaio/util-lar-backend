package com.example.product_api.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.product_api.dto.ProductDTO.ProductRequestDTO;
import com.example.product_api.dto.ProductDTO.ProductResponseDTO;
import com.example.product_api.dto.ProductDTO.AvailableProductRequestDTO;
import com.example.product_api.dto.ProductDTO.SoldProductRequestDTO;
import com.example.product_api.exception.NotFoundException;
import com.example.product_api.model.Product;
import com.example.product_api.model.ProductDimensions;
import com.example.product_api.util.SlugUtil;
import com.example.product_api.mapper.ProductMapper;
import com.example.product_api.repository.ProductRepository;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private static final String DEFAULT_NAME = "Produto sem nome";
    private static final String DEFAULT_SLUG = "produto-sem-nome";
    private static final int DEFAULT_LIST_PAGE = 0;
    private static final int DEFAULT_LIST_PAGE_SIZE = 24;
    private static final int MAX_LIST_PAGE_SIZE = 100;
    private final ConcurrentMap<Integer, CompletableFuture<ProductResponseDTO>> inFlightCreateRequests = new ConcurrentHashMap<>();

    @Autowired
    private final ProductRepository repository;
    @Autowired
    private final ProductMapper mapper;
    @Autowired
    private final ProductNotificationService productNotificationService;

    public ProductService(ProductRepository productRepository,
                          ProductMapper productMapper,
                          ProductNotificationService productNotificationService){
        this.repository = productRepository;
        this.mapper = productMapper;
        this.productNotificationService = productNotificationService;
    }

    public List<ProductResponseDTO> findAll(String category,
                                            String condition,
                                            Boolean featured,
                                            String soldDate,
                                            Integer page,
                                            Integer size){
        int resolvedPage = page != null && page >= 0 ? page : DEFAULT_LIST_PAGE;
        int resolvedSize = size != null ? Math.min(Math.max(size, 1), MAX_LIST_PAGE_SIZE) : DEFAULT_LIST_PAGE_SIZE;

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize);
        Page<?> productPage = repository.findListPage(category, condition, featured, soldDate, pageable);

        return productPage.getContent().stream()
                .map(com.example.product_api.repository.ProductListProjection.class::cast)
                .map(mapper::toListResponse)
            .toList();
    }

    public ProductResponseDTO findById(Long id) {
        return repository.findDetailById(id)
                .map(mapper::toDetailResponse)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard", allEntries = true),
        @CacheEvict(value = "monthlyEvolution", allEntries = true),
        @CacheEvict(value = "products", allEntries = true)
    })
    public ProductResponseDTO create(ProductRequestDTO dto){
        int fingerprint = buildCreateFingerprint(dto);
        CompletableFuture<ProductResponseDTO> inFlight = new CompletableFuture<>();
        CompletableFuture<ProductResponseDTO> existingRequest = inFlightCreateRequests.putIfAbsent(fingerprint, inFlight);

        if (existingRequest != null) {
            logger.info("Duplicate create request joined in-flight operation: fingerprint={}", fingerprint);
            return awaitInFlightCreate(existingRequest);
        }

        try {
            ProductResponseDTO response = doCreate(dto);
            inFlight.complete(response);
            return response;
        } catch (RuntimeException ex) {
            inFlight.completeExceptionally(ex);
            throw ex;
        } finally {
            inFlightCreateRequests.remove(fingerprint, inFlight);
        }
    }

    private ProductResponseDTO doCreate(ProductRequestDTO dto) {
        Product p = new Product();
        mapper.populateEntityForCreate(dto, p);

        String slug = generateUniqueSlug(buildSlugFromName(p.getName()), null);

        logger.info("Creating product: name={}, slug={}, category={}, featured={}",
                p.getName(),
                slug,
                p.getCategory(),
                p.getFeatured());

        p.setSlug(slug);
        p.setSoldDate(null);
        p.setDisponivel(1);

        Product saved = repository.save(p);
        logger.info("Product created successfully: id={}, slug={}", saved.getId(), saved.getSlug());

        productNotificationService.notifyNewProduct(saved);
        logger.info("Product notification dispatched asynchronously: id={}, slug={}", saved.getId(), saved.getSlug());

        return mapper.toResponse(saved);
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard", allEntries = true),
        @CacheEvict(value = "monthlyEvolution", allEntries = true),
        @CacheEvict(value = "products", allEntries = true)
    })
    public ProductResponseDTO update(Long id, ProductRequestDTO dto){
        Product existing = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        Long previousPrice = existing.getPrice();

        String newSlug = existing.getSlug();
        if (hasText(dto != null ? dto.getName() : null)) {
            newSlug = generateUniqueSlug(buildSlugFromName(dto.getName()), id);
        }

        mapper.applyPartialUpdate(dto, existing);
        existing.setSlug(newSlug);

        Product saved = repository.save(existing);

        if (!Objects.equals(previousPrice, saved.getPrice())) {
            logger.info("Product price changed: id={}, slug={}, previousPrice={}, currentPrice={}",
                    saved.getId(),
                    saved.getSlug(),
                    previousPrice,
                    saved.getPrice());
            productNotificationService.notifyPriceUpdated(saved, previousPrice);
        }

        return mapper.toResponse(saved);
    }

    public Long delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Produto não encontrado");
        }
        repository.deleteById(id);
        return id;
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard", allEntries = true),
        @CacheEvict(value = "monthlyEvolution", allEntries = true),
        @CacheEvict(value = "products", allEntries = true)
    })
    public ProductResponseDTO markAsSold(Long id, SoldProductRequestDTO dto) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        Instant soldAt = dto != null && dto.getSoldDate() != null ? dto.getSoldDate() : Instant.now();
        existing.setSoldDate(soldAt);
        existing.setStock(0);
        existing.setDisponivel(0);

        Product saved = repository.save(existing);
        return mapper.toResponse(saved);
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard", allEntries = true),
        @CacheEvict(value = "monthlyEvolution", allEntries = true),
        @CacheEvict(value = "products", allEntries = true)
    })
    public ProductResponseDTO markAsAvailable(Long id, AvailableProductRequestDTO dto) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        int stock = dto != null && dto.getStock() != null
                ? dto.getStock()
                : (existing.getStock() != null && existing.getStock() > 0 ? existing.getStock() : 1);

        existing.setSoldDate(null);
        existing.setDisponivel(1);
        existing.setStock(stock);

        Product saved = repository.save(existing);
        return mapper.toResponse(saved);
    }

    private String buildSlugFromName(String productName) {
        String resolvedName = hasText(productName) ? productName.trim() : DEFAULT_NAME;
        String slug = SlugUtil.slugify(resolvedName);
        return hasText(slug) ? slug : DEFAULT_SLUG;
    }

    private String generateUniqueSlug(String baseSlug, Long currentProductId) {
        String candidate = baseSlug;
        int suffix = 2;

        while (slugExistsForAnotherProduct(candidate, currentProductId)) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private boolean slugExistsForAnotherProduct(String slug, Long currentProductId) {
        if (currentProductId == null) {
            return repository.existsBySlug(slug);
        }

        return repository.existsBySlugAndIdNot(slug, currentProductId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ProductResponseDTO awaitInFlightCreate(CompletableFuture<ProductResponseDTO> inFlight) {
        try {
            return inFlight.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Criacao de produto interrompida", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new CompletionException(cause);
        }
    }

    private int buildCreateFingerprint(ProductRequestDTO dto) {
        return Objects.hash(
                normalize(dto != null ? dto.getName() : null),
                normalize(dto != null ? dto.getDescription() : null),
                dto != null ? dto.getPrice() : null,
                dto != null ? dto.getOriginalPrice() : null,
                dto != null ? dto.getCostPrice() : null,
                normalize(dto != null ? dto.getCondition() : null),
                normalize(dto != null ? dto.getCategory() : null),
                dto != null && dto.getImages() != null ? dto.getImages().hashCode() : 0,
                dto != null ? dto.getStock() : null,
                dimensionsFingerprint(dto != null ? dto.getDimensions() : null),
                normalize(dto != null ? dto.getMaterial() : null),
                normalize(dto != null ? dto.getColor() : null),
                normalize(dto != null ? dto.getBrand() : null),
                normalize(dto != null ? dto.getWarranty() : null),
                dto != null ? dto.getFeatured() : null
        );
    }

    private int dimensionsFingerprint(ProductDimensions dimensions) {
        if (dimensions == null) {
            return 0;
        }

        return Objects.hash(
                dimensions.getWidth(),
                dimensions.getHeight(),
                dimensions.getDepth(),
                normalize(dimensions.getUnit())
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

}

