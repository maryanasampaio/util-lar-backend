package com.example.product_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.product_api.dto.SimulationDTO.CardBrandResponseDTO;
import com.example.product_api.service.SimulationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/card-brands", "/api/card-brands"})
public class CardBrandController {

    private final SimulationService simulationService;

    @GetMapping
    public ResponseEntity<List<CardBrandResponseDTO>> findAllActive() {
        return ResponseEntity.ok(simulationService.getActiveBrands());
    }
}
