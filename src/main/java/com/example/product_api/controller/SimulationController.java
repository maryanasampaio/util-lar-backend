package com.example.product_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.product_api.dto.SimulationDTO.SimulationRequestDTO;
import com.example.product_api.dto.SimulationDTO.SimulationResponseDTO;
import com.example.product_api.service.SimulationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping({"/simulations/calculate", "/api/simulations/calculate"})
    public ResponseEntity<SimulationResponseDTO> calculate(@Valid @RequestBody SimulationRequestDTO request) {
        return ResponseEntity.ok(simulationService.calculate(request));
    }
}
