package com.example.product_api.service;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.product_api.model.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

@Service
public class WhatsAppNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.whatsapp-number:+14155238886}")
    private String twilioWhatsappNumber;

    @Value("${twilio.whatsapp.messaging-service-sid:}")
    private String messagingServiceSid;

    @Value("${twilio.whatsapp.new-product-content-sid:}")
    private String newProductContentSid;

    @Value("${twilio.whatsapp.price-update-content-sid:}")
    private String priceUpdateContentSid;

    @Value("${twilio.whatsapp.status-callback-url:}")
    private String statusCallbackUrl;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private volatile boolean initialized = false;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void notifyNewProduct(Product product, List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            logger.info("WhatsApp notification skipped: no active recipients for productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);
            return;
        }

        if (!isConfigured()) {
            logger.warn("WhatsApp notification skipped: Twilio is not configured. productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);
            return;
        }

                boolean useTemplate = hasTemplate(newProductContentSid);
                String message = useTemplate ? null : formatNewProductMessage(product);
                String mediaUrl = useTemplate ? null : resolvePrimaryImageUrl(product).orElse(null);
        logger.info("WhatsApp notification started: productId={}, slug={}, recipientsCount={}, sender={}",
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null,
                recipients.size(),
                maskPhone(twilioWhatsappNumber));

                if (!useTemplate) {
                    logger.warn("WhatsApp notification is using free-form message for productId={}, slug={}. Outside the 24-hour WhatsApp window this may fail with error 63016. Configure twilio.whatsapp.new-product-content-sid to use an approved template.",
                        product != null ? product.getId() : null,
                        product != null ? product.getSlug() : null);
                }

                if (mediaUrl != null) {
            logger.info("WhatsApp notification will include media: productId={}, slug={}, mediaUrl={}",
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null,
                mediaUrl);
        }

        for (String recipient : recipients) {
            try {
                MessageDispatchResult result = useTemplate
                    ? sendContentTemplate(recipient, newProductContentSid, buildNewProductTemplateVariables(product))
                    : sendMessage(recipient, message, mediaUrl);
                logger.info("WhatsApp message accepted by Twilio: productId={}, slug={}, recipient={}, sid={}, initialStatus={}",
                        product != null ? product.getId() : null,
                        product != null ? product.getSlug() : null,
                        maskPhone(recipient),
                        result.sid(),
                        result.status());
            } catch (Exception ex) {
                logger.error("WhatsApp message failed: productId={}, slug={}, recipient={}, error={}",
                        product != null ? product.getId() : null,
                        product != null ? product.getSlug() : null,
                        maskPhone(recipient),
                        ex.getMessage(),
                        ex);
            }
        }

        logger.info("WhatsApp notification finished: productId={}, slug={}",
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null);
    }

    public void notifyPriceUpdated(Product product, Long previousPrice, List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            logger.info("WhatsApp price update notification skipped: no active recipients for productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);
            return;
        }

        if (!isConfigured()) {
            logger.warn("WhatsApp price update notification skipped: Twilio is not configured. productId={}, slug={}",
                    product != null ? product.getId() : null,
                    product != null ? product.getSlug() : null);
            return;
        }

                boolean useTemplate = hasTemplate(priceUpdateContentSid);
                String message = useTemplate ? null : formatPriceUpdatedMessage(product, previousPrice);
        logger.info("WhatsApp price update notification started: productId={}, slug={}, recipientsCount={}, sender={}",
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null,
                recipients.size(),
                maskPhone(twilioWhatsappNumber));

                if (!useTemplate) {
                    logger.warn("WhatsApp price update notification is using free-form message for productId={}, slug={}. Outside the 24-hour WhatsApp window this may fail with error 63016. Configure twilio.whatsapp.price-update-content-sid to use an approved template.",
                        product != null ? product.getId() : null,
                        product != null ? product.getSlug() : null);
                }

        for (String recipient : recipients) {
            try {
                MessageDispatchResult result = useTemplate
                    ? sendContentTemplate(recipient, priceUpdateContentSid, buildPriceUpdatedTemplateVariables(product, previousPrice))
                    : sendMessage(recipient, message, null);
                logger.info("WhatsApp price update message accepted by Twilio: productId={}, slug={}, recipient={}, sid={}, initialStatus={}",
                        product != null ? product.getId() : null,
                        product != null ? product.getSlug() : null,
                        maskPhone(recipient),
                        result.sid(),
                        result.status());
            } catch (Exception ex) {
                logger.error("WhatsApp price update message failed: productId={}, slug={}, recipient={}, error={}",
                        product != null ? product.getId() : null,
                        product != null ? product.getSlug() : null,
                        maskPhone(recipient),
                        ex.getMessage(),
                        ex);
            }
        }

        logger.info("WhatsApp price update notification finished: productId={}, slug={}",
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null);
    }

    public String sendTestMessage(String phoneNumber) {
        return sendMessage(phoneNumber, "Teste de notificacao WhatsApp configurado com sucesso.", null).sid();
    }

    public MessageDispatchResult sendMessage(String phoneNumber, String message) {
        return sendMessage(phoneNumber, message, null);
    }

    public MessageDispatchResult sendMessage(String phoneNumber, String message, String mediaUrl) {
        if (!isConfigured()) {
            throw new RuntimeException("Twilio nao configurado. Defina TWILIO_ACCOUNT_SID e TWILIO_AUTH_TOKEN.");
        }

        initializeTwilio();

        String to = toWhatsAppAddress(phoneNumber);
        String from = toWhatsAppAddress(twilioWhatsappNumber);

        logger.info("Creating Twilio WhatsApp message: to={}, from={}, messageLength={}",
                maskPhone(phoneNumber),
                maskPhone(twilioWhatsappNumber),
                message != null ? message.length() : 0);

        var creator = Message.creator(
            new PhoneNumber(to),
            new PhoneNumber(from),
            message
        );

        if (notBlank(mediaUrl)) {
            creator.setMediaUrl(List.of(URI.create(mediaUrl)));
        }

        applyStatusCallback(creator);

        Message twilioMessage = creator.create();

        return toDispatchResult(twilioMessage);
    }

    public MessageDispatchResult sendContentTemplate(String phoneNumber, String contentSid, Map<String, String> variables) {
        if (!isConfigured()) {
            throw new RuntimeException("Twilio nao configurado. Defina TWILIO_ACCOUNT_SID e TWILIO_AUTH_TOKEN.");
        }

        if (!hasTemplate(contentSid)) {
            throw new RuntimeException("Template WhatsApp nao configurado. Defina o Content SID correspondente.");
        }

        initializeTwilio();

        String to = toWhatsAppAddress(phoneNumber);
        String from = toWhatsAppAddress(twilioWhatsappNumber);

        logger.info("Creating Twilio WhatsApp templated message: to={}, from={}, contentSid={}, hasMessagingServiceSid={}, variablesCount={}",
                maskPhone(phoneNumber),
                maskPhone(twilioWhatsappNumber),
                contentSid,
                notBlank(messagingServiceSid),
                variables != null ? variables.size() : 0);

        var creator = Message.creator(new PhoneNumber(to), new PhoneNumber(from), "");
        creator.setContentSid(contentSid);
        creator.setContentVariables(toJson(variables));

        if (notBlank(messagingServiceSid)) {
            creator.setMessagingServiceSid(messagingServiceSid);
        }

        applyStatusCallback(creator);

        Message twilioMessage = creator.create();
        return toDispatchResult(twilioMessage);
    }

    public void handleStatusCallback(String messageSid, String messageStatus, String to, String errorCode, String errorMessage) {
        String normalizedStatus = safe(messageStatus);
        if ("delivered".equalsIgnoreCase(normalizedStatus)
                || "read".equalsIgnoreCase(normalizedStatus)
                || "sent".equalsIgnoreCase(normalizedStatus)) {
            logger.info("Twilio WhatsApp delivery status: sid={}, status={}, recipient={}",
                    safe(messageSid),
                    normalizedStatus,
                    maskPhone(to));
            return;
        }

        if ("failed".equalsIgnoreCase(normalizedStatus)
                || "undelivered".equalsIgnoreCase(normalizedStatus)) {
            logger.warn("Twilio WhatsApp delivery failed: sid={}, status={}, recipient={}, errorCode={}, errorMessage={}",
                    safe(messageSid),
                    normalizedStatus,
                    maskPhone(to),
                    safe(errorCode),
                    safe(errorMessage));
            return;
        }

        logger.info("Twilio WhatsApp status callback received: sid={}, status={}, recipient={}, errorCode={}",
                safe(messageSid),
                normalizedStatus,
                maskPhone(to),
                safe(errorCode));
    }

    public String formatNewProductMessage(Product product) {
        String productUrl = buildProductUrl(product);
        return "Novo produto disponivel: " + product.getName()
            + "\nPreco: " + formatCurrency(product.getPrice())
            + "\nCategoria: " + safe(product.getCategory())
            + "\nVer produto: " + productUrl;
    }

    public String formatPriceUpdatedMessage(Product product, Long previousPrice) {
        String productUrl = buildProductUrl(product);
        return "Preco atualizado: " + product.getName()
            + "\nDe: " + formatCurrency(previousPrice)
            + "\nPara: " + formatCurrency(product.getPrice())
            + "\nCategoria: " + safe(product.getCategory())
            + "\nVer produto: " + productUrl;
    }

    private Map<String, String> buildNewProductTemplateVariables(Product product) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("1", safe(product != null ? product.getName() : null));
        variables.put("2", formatCurrency(product != null ? product.getPrice() : null));
        variables.put("3", safe(product != null ? product.getCategory() : null));
        variables.put("4", buildProductUrl(product));
        return variables;
    }

    private Map<String, String> buildPriceUpdatedTemplateVariables(Product product, Long previousPrice) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("1", safe(product != null ? product.getName() : null));
        variables.put("2", formatCurrency(previousPrice));
        variables.put("3", formatCurrency(product != null ? product.getPrice() : null));
        variables.put("4", safe(product != null ? product.getCategory() : null));
        variables.put("5", buildProductUrl(product));
        return variables;
    }

    private String buildProductUrl(Product product) {
        if (product == null || !notBlank(product.getSlug())) {
            return frontendUrl;
        }

        String normalizedFrontendUrl = frontendUrl == null ? "" : frontendUrl.trim();
        while (normalizedFrontendUrl.endsWith("/")) {
            normalizedFrontendUrl = normalizedFrontendUrl.substring(0, normalizedFrontendUrl.length() - 1);
        }

        return normalizedFrontendUrl + "/produtos/" + product.getSlug();
    }

    private boolean hasTemplate(String contentSid) {
        return notBlank(contentSid);
    }

    private Optional<String> resolvePrimaryImageUrl(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return Optional.empty();
        }

        return product.getImages().stream()
                .filter(this::isAbsoluteHttpUrl)
                .findFirst();
    }

    private boolean isAbsoluteHttpUrl(String value) {
        if (!notBlank(value)) {
            return false;
        }

        String normalizedValue = value.trim().toLowerCase();
        return normalizedValue.startsWith("http://") || normalizedValue.startsWith("https://");
    }

    private String formatCurrency(Long cents) {
        if (cents == null) {
            return "R$ 0,00";
        }
        BigDecimal reais = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return "R$ " + reais.toPlainString().replace('.', ',');
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private String toWhatsAppAddress(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new RuntimeException("Numero de telefone invalido");
        }

        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            throw new RuntimeException("Numero de telefone invalido");
        }

        return "whatsapp:+" + digits;
    }

    private String toJson(Map<String, String> variables) {
        try {
            return objectMapper.writeValueAsString(variables == null ? Map.of() : variables);
        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException("Falha ao serializar variaveis do template WhatsApp", ex);
        }
    }

    private void applyStatusCallback(MessageCreator creator) {
        if (notBlank(statusCallbackUrl)) {
            creator.setStatusCallback(URI.create(statusCallbackUrl.trim()));
        }
    }

    private MessageDispatchResult toDispatchResult(Message twilioMessage) {
        String status = twilioMessage.getStatus() == null ? "unknown" : twilioMessage.getStatus().toString();
        return new MessageDispatchResult(twilioMessage.getSid(), status);
    }

    private boolean isConfigured() {
        return notBlank(accountSid) && notBlank(authToken);
    }

    private synchronized void initializeTwilio() {
        if (initialized) {
            logger.debug("Twilio already initialized for accountSid={}", maskAccountSid(accountSid));
            return;
        }

        logger.info("Initializing Twilio client: accountSid={}, sender={}",
                maskAccountSid(accountSid),
                maskPhone(twilioWhatsappNumber));
        Twilio.init(accountSid, authToken);
        initialized = true;
        logger.info("Twilio client initialized successfully: accountSid={}", maskAccountSid(accountSid));
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String maskPhone(String phoneNumber) {
        if (!notBlank(phoneNumber)) {
            return "<empty>";
        }

        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) {
            return "****" + digits;
        }

        return "****" + digits.substring(digits.length() - 4);
    }

    private String maskAccountSid(String sid) {
        if (!notBlank(sid)) {
            return "<empty>";
        }

        if (sid.length() <= 6) {
            return sid;
        }

        return sid.substring(0, 4) + "..." + sid.substring(sid.length() - 2);
    }

    public record MessageDispatchResult(String sid, String status) {
    }
}
