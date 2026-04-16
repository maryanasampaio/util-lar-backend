package com.example.product_api.dto.SimulationDTO;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequestDTO {

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "installments is required")
    @Min(value = 1, message = "installments must be between 1 and 12")
    @Max(value = 12, message = "installments must be between 1 and 12")
    private Integer installments;

    @NotBlank(message = "card_brand is required")
    @JsonProperty("card_brand")
    private String cardBrand;

    @NotNull(message = "repasse is required")
    private Boolean repasse;
}
