package com.example.product_api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.product_api.dto.NotificationDTO.NotificationPreferencesRequestDTO;
import com.example.product_api.dto.NotificationDTO.NotificationPreferencesResponseDTO;
import com.example.product_api.dto.NotificationDTO.PushSubscriptionRequestDTO;
import com.example.product_api.service.NotificationPreferenceService;
import com.example.product_api.service.PushNotificationService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping({"/api/notification-preferences", "/notification-preferences"})
public class NotificationPreferencesController {

    private final NotificationPreferenceService notificationPreferenceService;
    private final PushNotificationService pushNotificationService;

    @GetMapping
    public NotificationPreferencesResponseDTO getPreferences(@RequestParam(required = false) String whatsappPhone) {
        return notificationPreferenceService.getPreferences(whatsappPhone);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> savePreferences(
            @Valid @RequestBody NotificationPreferencesRequestDTO dto) {
        notificationPreferenceService.savePreferences(dto);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Preferencias salvas com sucesso"
        ));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @Valid @RequestBody NotificationPreferencesRequestDTO dto) {
        notificationPreferenceService.savePreferences(dto);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Preferencias atualizadas com sucesso"
        ));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deletePreferences(@RequestParam String whatsappPhone) {
        notificationPreferenceService.deletePreferences(whatsappPhone);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Preferencias removidas com sucesso"
        ));
    }

    @PostMapping("/push-subscription")
    public ResponseEntity<Map<String, Object>> savePushSubscription(
            @Valid @RequestBody PushSubscriptionRequestDTO dto) {
        pushNotificationService.saveSubscription(dto);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Push subscription salva com sucesso"
        ));
    }

    @DeleteMapping("/push-subscription")
    public ResponseEntity<Map<String, Object>> deletePushSubscription(@RequestParam String endpoint) {
        pushNotificationService.deleteSubscription(endpoint);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Push subscription removida com sucesso"
        ));
    }
}
