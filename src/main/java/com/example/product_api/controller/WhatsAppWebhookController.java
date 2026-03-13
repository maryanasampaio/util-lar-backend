package com.example.product_api.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.product_api.service.NotificationPreferenceService;

@RestController
@RequestMapping({"/api/whatsapp", "/whatsapp"})
public class WhatsAppWebhookController {

    private final NotificationPreferenceService notificationPreferenceService;

    public WhatsAppWebhookController(NotificationPreferenceService notificationPreferenceService) {
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> receiveIncomingMessage(
            @RequestParam(name = "From", required = false) String from,
            @RequestParam(name = "Body", required = false) String body) {
        String phone = notificationPreferenceService.subscribeFromIncomingWhatsapp(from);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "capturedPhone", phone,
            "receivedMessage", body == null ? "" : body
        ));
    }
}