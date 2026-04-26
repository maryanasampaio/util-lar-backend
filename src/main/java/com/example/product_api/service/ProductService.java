package com.example.product_api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.product_api.dto.ProductDTO.ProductRequestDTO;
import com.example.product_api.dto.ProductDTO.ProductResponseDTO;
import com.example.product_api.dto.ProductDTO.AvailableProductRequestDTO;
import com.example.product_api.dto.ProductDTO.SoldProductRequestDTO;
import com.example.product_api.exception.NotFoundException;
import com.example.product_api.model.Product;
import com.example.product_api.util.SlugUtil;
import com.example.product_api.mapper.ProductMapper;
import com.example.product_api.repository.ProductRepository;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private static final String DEFAULT_NAME = "Produto sem nome";
    private static final String DEFAULT_SLUG = "produto-sem-nome";
    private static final int FIND_ALL_PAGE_SIZE = 100;

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

    public List<ProductResponseDTO> findAll(String category, String condition, Boolean featured, String soldDate){
        Specification<Product> spec = Specification.where(null);

        if (category != null && !category.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }

        if (condition != null && !condition.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("condition"), condition));
        }

        if (featured != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("featured"), featured));
        }

        if ("null".equalsIgnoreCase(soldDate)) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("soldDate")));
        }

        List<ProductResponseDTO> products = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, FIND_ALL_PAGE_SIZE);
        Page<Product> page;

        do {
            page = repository.findAll(spec, pageable);
            page.getContent().stream()
                    .map(mapper::toListResponse)
                    .forEach(products::add);
            pageable = page.nextPageable();
        } while (page.hasNext());

        return products;
    }

    public ProductResponseDTO findById(Long id) {
        Product p = repository.findById(id).orElseThrow(() -> new NotFoundException("Produto não encontrado"));
        return mapper.toResponse(p);
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard", allEntries = true),
        @CacheEvict(value = "monthlyEvolution", allEntries = true),
        @CacheEvict(value = "products", allEntries = true)
    })
    public ProductResponseDTO create(ProductRequestDTO dto){
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

}

