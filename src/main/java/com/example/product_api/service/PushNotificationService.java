package com.example.product_api.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.product_api.dto.NotificationDTO.PushSubscriptionRequestDTO;
import com.example.product_api.model.Product;
import com.example.product_api.model.PushSubscription;
import com.example.product_api.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${webpush.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${webpush.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${webpush.vapid.subject:}")
    private String vapidSubject;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PushNotificationService(PushSubscriptionRepository pushSubscriptionRepository) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    public void saveSubscription(PushSubscriptionRequestDTO dto) {
        String endpoint = normalizeEndpoint(dto.getEndpoint());
        if (endpoint == null) {
            logger.warn("Push subscription rejected: missing endpoint");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endpoint e obrigatorio");
        }

        logger.info("Saving push subscription: endpointHash={}, hasP256dh={}, hasAuth={}, expirationTime={}",
                maskEndpoint(endpoint),
                dto.getKeys() != null && dto.getKeys().getP256dh() != null && !dto.getKeys().getP256dh().isBlank(),
                dto.getKeys() != null && dto.getKeys().getAuth() != null && !dto.getKeys().getAuth().isBlank(),
                dto.getExpirationTime());

        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(endpoint)
            .orElseGet(PushSubscription::new);

        subscription.setEndpoint(endpoint);
        subscription.setPublicKey(dto.getKeys().getP256dh().trim());
        subscription.setAuthSecret(dto.getKeys().getAuth().trim());
        subscription.setExpirationTime(dto.getExpirationTime());

        pushSubscriptionRepository.save(subscription);
        logger.info("Push subscription saved successfully: endpointHash={}, totalSubscriptions={}",
                maskEndpoint(endpoint),
                pushSubscriptionRepository.count());
    }

    public void deleteSubscription(String endpoint) {
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        if (normalizedEndpoint == null) {
            logger.warn("Push subscription delete rejected: missing endpoint");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endpoint e obrigatorio");
        }

        pushSubscriptionRepository.deleteByEndpoint(normalizedEndpoint);
        logger.info("Push subscription deleted: endpointHash={}, totalSubscriptions={}",
                maskEndpoint(normalizedEndpoint),
                pushSubscriptionRepository.count());
    }

    public void notifyNewProduct(Product product) {
        if (!isConfigured()) {
            logger.warn("Web Push skipped: VAPID is not configured. productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);
            return;
        }

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            logger.info("Web Push skipped: no subscriptions found for productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);
            return;
        }

        logger.info("Web Push notification started: productId={}, slug={}, subscriptionsCount={}",
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null,
                subscriptions.size());

        String payload = buildNewProductPayload(product);
        sendToAllSubscriptions(subscriptions, payload,
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null,
                "new-product");
    }

    public void notifyPriceUpdated(Product product, Long previousPrice) {
        if (!isConfigured()) {
            logger.warn("Web Push price update skipped: VAPID is not configured. productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);
            return;
        }

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            logger.info("Web Push price update skipped: no subscriptions found for productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);
            return;
        }

        logger.info("Web Push price update started: productId={}, slug={}, subscriptionsCount={}",
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null,
                subscriptions.size());

        String payload = buildPriceUpdatedPayload(product, previousPrice);
        sendToAllSubscriptions(subscriptions, payload,
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null,
                "price-update");
    }

    private void sendToAllSubscriptions(List<PushSubscription> subscriptions,
                                        String payload,
                                        Long productId,
                                        String slug,
                                        String notificationType) {
        PushService pushService = createPushService();

        for (PushSubscription subscription : subscriptions) {
            try {
                Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getPublicKey(),
                    subscription.getAuthSecret(),
                    payload
                );

                HttpResponse response = pushService.send(notification);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine == null ? 0 : statusLine.getStatusCode();

                if (statusCode == 404 || statusCode == 410) {
                    logger.info("Web Push subscription expired and will be removed: productId={}, slug={}, endpointHash={}, statusCode={}",
                            productId,
                            slug,
                            maskEndpoint(subscription.getEndpoint()),
                            statusCode);
                    pushSubscriptionRepository.deleteByEndpoint(subscription.getEndpoint());
                    continue;
                }

                logger.info("Web Push accepted: type={}, productId={}, slug={}, endpointHash={}, statusCode={}",
                        notificationType,
                        productId,
                        slug,
                        maskEndpoint(subscription.getEndpoint()),
                        statusCode);
            } catch (GeneralSecurityException | IOException | JoseException | ExecutionException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                logger.error("Web Push failed: type={}, productId={}, slug={}, endpointHash={}, error={}",
                        notificationType,
                        productId,
                        slug,
                        maskEndpoint(subscription.getEndpoint()),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    private String buildNewProductPayload(Product product) {
        Map<String, Object> notification = baseNotificationPayload(
            "Novo produto disponivel",
            safe(product != null ? product.getName() : null) + " por " + formatCurrency(product != null ? product.getPrice() : null),
            buildProductUrl(product),
            product != null && product.getId() != null ? "product-new-" + product.getId() : "product-new"
        );
        notification.put("body", safe(product != null ? product.getName() : null)
                + "\nPreco: " + formatCurrency(product != null ? product.getPrice() : null)
                + "\nCategoria: " + safe(product != null ? product.getCategory() : null));
        return toNotificationEnvelope(notification);
    }

    private String buildPriceUpdatedPayload(Product product, Long previousPrice) {
        Map<String, Object> notification = baseNotificationPayload(
            "Preco atualizado",
            safe(product != null ? product.getName() : null) + " agora custa " + formatCurrency(product != null ? product.getPrice() : null),
            buildProductUrl(product),
            product != null && product.getId() != null ? "product-price-" + product.getId() : "product-price"
        );
        notification.put("body", safe(product != null ? product.getName() : null)
                + "\nDe: " + formatCurrency(previousPrice)
                + "\nPara: " + formatCurrency(product != null ? product.getPrice() : null));
        return toNotificationEnvelope(notification);
    }

    private Map<String, Object> baseNotificationPayload(String title, String body, String productUrl, String tag) {
        Map<String, Object> onActionClick = new LinkedHashMap<>();
        onActionClick.put("default", Map.of(
            "operation", "openWindow",
            "url", productUrl
        ));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", productUrl);
        data.put("onActionClick", onActionClick);

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("tag", tag);
        notification.put("data", data);
        return notification;
    }

    private String toNotificationEnvelope(Map<String, Object> notification) {
        try {
            return objectMapper.writeValueAsString(Map.of("notification", notification));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload de Web Push", ex);
        }
    }

    private PushService createPushService() {
        try {
            ensureBouncyCastle();
            return new PushService(vapidPublicKey.trim(), vapidPrivateKey.trim(), vapidSubject.trim());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao inicializar Web Push com as chaves VAPID", ex);
        }
    }

    private void ensureBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private boolean isConfigured() {
        return notBlank(vapidPublicKey) && notBlank(vapidPrivateKey) && notBlank(vapidSubject);
    }

    private String buildProductUrl(Product product) {
        String normalizedFrontendUrl = frontendUrl == null ? "" : frontendUrl.trim();
        while (normalizedFrontendUrl.endsWith("/")) {
            normalizedFrontendUrl = normalizedFrontendUrl.substring(0, normalizedFrontendUrl.length() - 1);
        }

        if (product == null || !notBlank(product.getSlug())) {
            return normalizedFrontendUrl;
        }

        return normalizedFrontendUrl + "/produtos/" + product.getSlug();
    }

    private String normalizeEndpoint(String endpoint) {
        if (!notBlank(endpoint)) {
            return null;
        }
        return endpoint.trim();
    }

    private String formatCurrency(Long cents) {
        if (cents == null) {
            return "R$ 0,00";
        }

        long abs = Math.abs(cents);
        long reais = abs / 100;
        long centavos = abs % 100;
        String prefix = cents < 0 ? "-" : "";
        return prefix + "R$ " + reais + "," + String.format("%02d", centavos);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Produto" : value;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String maskEndpoint(String endpoint) {
        if (!notBlank(endpoint)) {
            return "<empty>";
        }

        int hash = endpoint.trim().hashCode();
        return Integer.toHexString(hash);
    }
}