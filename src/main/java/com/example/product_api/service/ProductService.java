package com.example.product_api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private final ProductImageService imageService;

    public ProductService(ProductRepository productRepository,
                          ProductMapper productMapper,
                          ProductNotificationService productNotificationService,
                          ProductImageService imageService){
        this.repository = productRepository;
        this.mapper = productMapper;
        this.productNotificationService = productNotificationService;
        this.imageService = imageService;
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

        List<String> incomingImages = p.getImages();
        boolean needsUpload = imageService.hasLegacyImage(incomingImages);
        if (needsUpload) {
            p.setImages(null);
        } else {
            p.setImages(imageService.normalizeForStorage(null, incomingImages));
        }

        Product saved = repository.save(p);
        if (needsUpload) {
            try {
                saved.setImages(imageService.normalizeForStorage(saved.getId(), incomingImages));
                saved = repository.save(saved);
            } catch (RuntimeException ex) {
                logger.error("Product image upload failed, rolling back product: id={}, reason={}",
                        saved.getId(), ex.getMessage());
                imageService.deleteAll(saved.getId());
                repository.deleteById(saved.getId());
                throw ex;
            }
        }
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
        List<String> previousImages = existing.getImages() != null ? List.copyOf(existing.getImages()) : null;

        String newSlug = existing.getSlug();
        if (hasText(dto != null ? dto.getName() : null)) {
            newSlug = generateUniqueSlug(buildSlugFromName(dto.getName()), id);
        }

        mapper.applyPartialUpdate(dto, existing);
        existing.setSlug(newSlug);

        if (dto != null && dto.getImages() != null) {
            existing.setImages(imageService.normalizeForStorage(id, dto.getImages()));
        }

        Product saved = repository.save(existing);

        if (dto != null && dto.getImages() != null) {
            imageService.deleteRemoved(previousImages, saved.getImages());
        }

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
        imageService.deleteAll(id);
        return id;
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard", allEntries = true),
        @CacheEvict(value = "monthlyEvolution", allEntries = true),
        @CacheEvict(value = "products", allEntries = true)
    })
    public Map<String, Object> migrateLegacyImages(Integer limit) {
        if (!imageService.isStorageConfigured()) {
            throw new IllegalStateException(
                    "Storage S3 nao configurado neste ambiente: migracao nao executada. "
                    + "Defina S3_URL, S3_BUCKET, S3_KEY_ID e S3_SECRET_KEY.");
        }

        int resolvedLimit = limit != null && limit > 0 ? limit : Integer.MAX_VALUE;
        List<Long> ids = repository.findIdsWithLegacyImages();

        int processed = 0;
        int convertedImages = 0;
        List<Map<String, Object>> failures = new ArrayList<>();

        for (Long id : ids) {
            if (processed >= resolvedLimit) {
                break;
            }
            processed++;
            try {
                Product product = repository.findById(id).orElse(null);
                if (product == null || !imageService.hasLegacyImage(product.getImages())) {
                    continue;
                }
                int before = (int) product.getImages().stream().filter(imageService::isDataUrl).count();
                product.setImages(imageService.normalizeForStorage(id, product.getImages()));
                repository.save(product);
                convertedImages += before;
                logger.info("Legacy images migrated: productId={}, images={}", id, before);
            } catch (RuntimeException ex) {
                logger.warn("Legacy image migration failed: productId={}, reason={}", id, ex.getMessage());
                failures.add(Map.of("id", id, "error", String.valueOf(ex.getMessage())));
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("pendentesAntes", ids.size());
        report.put("processados", processed);
        report.put("imagensConvertidas", convertedImages);
        report.put("falhas", failures);
        report.put("pendentesDepois", repository.findIdsWithLegacyImages().size());
        return report;
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

