package com.example.product_api.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.product_api.model.Product;
import com.example.product_api.repository.NotificationPreferenceRepository;

@Service
public class ProductNotificationService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;

    public ProductNotificationService(NotificationPreferenceRepository notificationPreferenceRepository,
                                      WhatsAppNotificationService whatsAppNotificationService) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.whatsAppNotificationService = whatsAppNotificationService;
    }

    @Async
    public void notifyNewProduct(Product product) {
        try {
            List<String> recipients = notificationPreferenceRepository.findAllActiveWhatsappPhones();
            whatsAppNotificationService.notifyNewProduct(product, recipients);
        } catch (Exception ex) {
            System.err.println("Erro ao enviar notificacoes de novo produto: " + ex.getMessage());
        }
    }
}
