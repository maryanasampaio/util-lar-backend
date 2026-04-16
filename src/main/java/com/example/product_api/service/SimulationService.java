package com.example.product_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.product_api.dto.SimulationDTO.CardBrandResponseDTO;
import com.example.product_api.dto.SimulationDTO.SimulationRequestDTO;
import com.example.product_api.dto.SimulationDTO.SimulationResponseDTO;
import com.example.product_api.exception.NotFoundException;
import com.example.product_api.model.CardFeeRate;
import com.example.product_api.repository.CardBrandRepository;
import com.example.product_api.repository.CardFeeRateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final CardBrandRepository cardBrandRepository;
    private final CardFeeRateRepository cardFeeRateRepository;

    public SimulationResponseDTO calculate(SimulationRequestDTO request) {
        String normalizedBrand = request.getCardBrand().trim().toLowerCase();

        if (!cardBrandRepository.existsByIdAndActiveTrue(normalizedBrand)) {
            throw new NotFoundException("RATE_NOT_FOUND: card_brand not found or inactive");
        }

        CardFeeRate rate = cardFeeRateRepository
            .findByCardBrand_IdAndInstallmentsAndRepasseAndActiveTrue(
                normalizedBrand,
                request.getInstallments(),
                request.getRepasse())
            .orElseThrow(() -> new NotFoundException("RATE_NOT_FOUND: rate not configured for this scenario"));

        BigDecimal amount = request.getAmount();
        BigDecimal feePercent = rate.getRatePercent();
        BigDecimal feeDecimal = feePercent.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);

        if (feeDecimal.compareTo(ONE) >= 0) {
            throw new IllegalArgumentException("VALIDATION_ERROR: rate_percent must be less than 100");
        }

        if (Boolean.TRUE.equals(request.getRepasse())) {
            return withRepasse(amount, request.getInstallments(), feeDecimal, normalizedBrand);
        }

        return withoutRepasse(amount, request.getInstallments(), feeDecimal, normalizedBrand);
    }

    public List<CardBrandResponseDTO> getActiveBrands() {
        return cardBrandRepository.findByActiveTrueOrderByNameAsc()
            .stream()
            .map(b -> new CardBrandResponseDTO(b.getId(), b.getName()))
            .toList();
    }

    private SimulationResponseDTO withRepasse(BigDecimal amount, Integer installments, BigDecimal feeDecimal, String cardBrand) {
        BigDecimal finalAmount = amount.divide(ONE.subtract(feeDecimal), 10, RoundingMode.HALF_UP);
        BigDecimal operatorFee = finalAmount.subtract(amount);

        return new SimulationResponseDTO(
            round2(amount),
            round2(finalAmount),
            round2(finalAmount.divide(BigDecimal.valueOf(installments), 10, RoundingMode.HALF_UP)),
            round2(operatorFee),
            round2(operatorFee),
            round2(amount),
            true,
            cardBrand,
            installments
        );
    }

    private SimulationResponseDTO withoutRepasse(BigDecimal amount, Integer installments, BigDecimal feeDecimal, String cardBrand) {
        BigDecimal operatorFee = amount.multiply(feeDecimal);

        return new SimulationResponseDTO(
            round2(amount),
            round2(amount),
            round2(amount.divide(BigDecimal.valueOf(installments), 10, RoundingMode.HALF_UP)),
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
            round2(operatorFee),
            round2(amount.subtract(operatorFee)),
            false,
            cardBrand,
            installments
        );
    }

    private BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
