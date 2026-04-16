package com.example.product_api.dto.SimulationDTO;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponseDTO {

    @JsonProperty("base_amount")
    private BigDecimal baseAmount;

    @JsonProperty("final_amount")
    private BigDecimal finalAmount;

    @JsonProperty("installment_value")
    private BigDecimal installmentValue;

    @JsonProperty("interest_total")
    private BigDecimal interestTotal;

    @JsonProperty("operator_fee")
    private BigDecimal operatorFee;

    @JsonProperty("net_amount")
    private BigDecimal netAmount;

    private Boolean repasse;

    @JsonProperty("card_brand")
    private String cardBrand;

    private Integer installments;
}
