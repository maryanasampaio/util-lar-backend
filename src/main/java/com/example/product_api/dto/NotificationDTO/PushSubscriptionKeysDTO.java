package com.example.product_api.dto.NotificationDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionKeysDTO {

    @NotBlank(message = "keys.p256dh e obrigatorio")
    private String p256dh;

    @NotBlank(message = "keys.auth e obrigatorio")
    private String auth;
}