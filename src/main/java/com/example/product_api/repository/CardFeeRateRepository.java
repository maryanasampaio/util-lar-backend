package com.example.product_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.product_api.model.CardFeeRate;

public interface CardFeeRateRepository extends JpaRepository<CardFeeRate, Long> {
    Optional<CardFeeRate> findByCardBrand_IdAndInstallmentsAndRepasseAndActiveTrue(
        String cardBrand,
        Integer installments,
        Boolean repasse
    );

    List<CardFeeRate> findAllByCardBrand_IdAndActiveTrue(String cardBrand);
}
