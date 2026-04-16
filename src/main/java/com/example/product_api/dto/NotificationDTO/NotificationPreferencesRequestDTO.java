package com.example.product_api.dto.NotificationDTO;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesRequestDTO {

    private Boolean whatsappEnabled;

    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "whatsappPhone deve conter apenas numeros (10 a 15 digitos)")
    private String whatsappPhone;
}
