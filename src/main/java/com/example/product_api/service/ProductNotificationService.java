package com.example.product_api.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.product_api.model.Product;
import com.example.product_api.repository.NotificationPreferenceRepository;

@Service
public class ProductNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ProductNotificationService.class);

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
            logger.info("Product notification flow started: productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);

            List<String> recipients = notificationPreferenceRepository.findAllActiveWhatsappPhones();
            logger.info("Active WhatsApp recipients loaded: productId={}, slug={}, recipientsCount={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null,
                    recipients != null ? recipients.size() : 0);

            whatsAppNotificationService.notifyNewProduct(product, recipients);
        } catch (Exception ex) {
            logger.error("Product notification flow failed: productId={}, slug={}, error={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null,
                    ex.getMessage(),
                    ex);
        }
    }
}
