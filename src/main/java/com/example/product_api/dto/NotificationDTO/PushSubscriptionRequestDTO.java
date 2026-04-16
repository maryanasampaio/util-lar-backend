package com.example.product_api.dto.NotificationDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionRequestDTO {

    @NotBlank(message = "endpoint e obrigatorio")
    private String endpoint;

    private Long expirationTime;

    @Valid
    @NotNull(message = "keys e obrigatorio")
    private PushSubscriptionKeysDTO keys;
}