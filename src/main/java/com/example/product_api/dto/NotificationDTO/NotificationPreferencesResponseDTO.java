package com.example.product_api.dto.NotificationDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesResponseDTO {

    private Boolean whatsappEnabled;
    private String whatsappPhone;
}
