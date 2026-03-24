package com.example.product_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.product_api.model.Product;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
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

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private volatile boolean initialized = false;

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

        String message = formatNewProductMessage(product);
        logger.info("WhatsApp notification started: productId={}, slug={}, recipientsCount={}, sender={}",
                product != null ? product.getId() : null,
                product != null ? product.getSlug() : null,
                recipients.size(),
                maskPhone(twilioWhatsappNumber));

        for (String recipient : recipients) {
            try {
                String sid = sendMessage(recipient, message);
                logger.info("WhatsApp message sent successfully: productId={}, slug={}, recipient={}, sid={}",
                        product != null ? product.getId() : null,
                        product != null ? product.getSlug() : null,
                        maskPhone(recipient),
                        sid);
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

    public String sendTestMessage(String phoneNumber) {
        return sendMessage(phoneNumber, "Teste de notificacao WhatsApp configurado com sucesso.");
    }

    public String sendMessage(String phoneNumber, String message) {
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

        Message twilioMessage = Message.creator(
            new PhoneNumber(to),
            new PhoneNumber(from),
            message
        ).create();

        return twilioMessage.getSid();
    }

    public String formatNewProductMessage(Product product) {
        String productUrl = frontendUrl + "/produtos/" + product.getSlug();
        return "Novo produto disponivel: " + product.getName()
            + "\nPreco: " + formatCurrency(product.getPrice())
            + "\nCategoria: " + safe(product.getCategory())
            + "\nVer produto: " + productUrl;
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
}
