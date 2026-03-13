package com.example.product_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.product_api.model.Product;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
public class WhatsAppNotificationService {

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
        if (recipients == null || recipients.isEmpty() || !isConfigured()) {
            return;
        }

        String message = formatNewProductMessage(product);

        for (String recipient : recipients) {
            try {
                sendMessage(recipient, message);
            } catch (Exception ex) {
                System.err.println("Falha ao enviar WhatsApp para " + recipient + ": " + ex.getMessage());
            }
        }
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
            return;
        }
        Twilio.init(accountSid, authToken);
        initialized = true;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
