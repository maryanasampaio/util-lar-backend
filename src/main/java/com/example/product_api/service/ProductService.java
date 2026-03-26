package com.example.product_api.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.product_api.dto.ProductDTO.ProductRequestDTO;
import com.example.product_api.dto.ProductDTO.ProductResponseDTO;
import com.example.product_api.dto.ProductDTO.AvailableProductRequestDTO;
import com.example.product_api.dto.ProductDTO.SoldProductRequestDTO;
import com.example.product_api.exception.ConflictException;
import com.example.product_api.exception.NotFoundException;
import com.example.product_api.model.Product;
import com.example.product_api.util.SlugUtil;
import com.example.product_api.mapper.ProductMapper;
import com.example.product_api.repository.ProductRepository;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

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

        return repository.findAll(spec)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
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
        String slug = SlugUtil.slugify(dto.getName());

        logger.info("Creating product: name={}, slug={}, category={}, featured={}",
                dto.getName(),
                slug,
                dto.getCategory(),
                dto.getFeatured());

        if (repository.existsBySlug(slug)) {
            throw new ConflictException("Produto com este nome já existe");
        }

        Product p = new Product();
        mapper.updateEntityFromDto(dto, p);
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

        String newSlug = SlugUtil.slugify(dto.getName());

        if (repository.existsBySlugAndIdNot(newSlug, id)) {
            throw new ConflictException("Produto com este nome já existe");
        }

        mapper.updateEntityFromDto(dto, existing);
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

}
