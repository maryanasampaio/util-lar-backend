package com.example.product_api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.product_api.dto.ProductDTO.ProductRequestDTO;
import com.example.product_api.dto.ProductDTO.ProductResponseDTO;
import com.example.product_api.dto.ProductDTO.AvailableProductRequestDTO;
import com.example.product_api.dto.ProductDTO.SoldProductRequestDTO;
import com.example.product_api.service.ProductService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping({"/api/products", "/produtos"})

public class ProductController {

    private final ProductService service;
    
    @GetMapping
    public List<ProductResponseDTO> findAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String soldDate) {
        return service.findAll(category, condition, featured, soldDate);
    }

    @GetMapping("/{id}")
    public ProductResponseDTO findById(@PathVariable Long id){
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> save(@Valid @RequestBody ProductRequestDTO product){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(product));
    }

    @PutMapping("/{id}")
    public ProductResponseDTO update(@PathVariable Long id, @Valid @RequestBody ProductRequestDTO product){
        return service.update(id, product);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id){
        Long deletedId = service.delete(id);
        return Map.of("message", "Produto removido com sucesso", "id", deletedId);
    }

    @PostMapping("/{id}/sold")
    public ProductResponseDTO markAsSold(@PathVariable Long id, @RequestBody(required = false) SoldProductRequestDTO soldRequest) {
        return service.markAsSold(id, soldRequest);
    }

    @PostMapping("/{id}/available")
    public ProductResponseDTO markAsAvailable(@PathVariable Long id, @RequestBody(required = false) AvailableProductRequestDTO availableRequest) {
        return service.markAsAvailable(id, availableRequest);
    }

}
