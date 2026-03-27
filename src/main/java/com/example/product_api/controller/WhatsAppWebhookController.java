package com.example.product_api.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.product_api.service.NotificationPreferenceService;
import com.example.product_api.service.WhatsAppNotificationService;

@RestController
@RequestMapping({"/api/whatsapp", "/whatsapp"})
public class WhatsAppWebhookController {

    private final NotificationPreferenceService notificationPreferenceService;
    private final WhatsAppNotificationService whatsAppNotificationService;

    public WhatsAppWebhookController(NotificationPreferenceService notificationPreferenceService,
                                     WhatsAppNotificationService whatsAppNotificationService) {
        this.notificationPreferenceService = notificationPreferenceService;
        this.whatsAppNotificationService = whatsAppNotificationService;
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

    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> receiveStatusCallback(
            @RequestParam(name = "MessageSid", required = false) String messageSid,
            @RequestParam(name = "MessageStatus", required = false) String messageStatus,
            @RequestParam(name = "To", required = false) String to,
            @RequestParam(name = "ErrorCode", required = false) String errorCode,
            @RequestParam(name = "ErrorMessage", required = false) String errorMessage) {
        whatsAppNotificationService.handleStatusCallback(messageSid, messageStatus, to, errorCode, errorMessage);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "messageSid", messageSid == null ? "" : messageSid,
            "messageStatus", messageStatus == null ? "" : messageStatus
        ));
    }
}