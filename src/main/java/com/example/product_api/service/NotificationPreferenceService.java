package com.example.product_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.product_api.dto.NotificationDTO.NotificationPreferencesRequestDTO;
import com.example.product_api.dto.NotificationDTO.NotificationPreferencesResponseDTO;
import com.example.product_api.model.NotificationPreference;
import com.example.product_api.repository.NotificationPreferenceRepository;

@Service
public class NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    public NotificationPreferencesResponseDTO getPreferences(String whatsappPhone) {
        String normalizedPhone = normalizePhone(whatsappPhone);
        if (normalizedPhone == null) {
            return new NotificationPreferencesResponseDTO(false, null);
        }

        return notificationPreferenceRepository.findByWhatsappPhone(normalizedPhone)
            .map(this::toResponse)
            .orElseGet(() -> new NotificationPreferencesResponseDTO(false, normalizedPhone));
    }

    public void savePreferences(NotificationPreferencesRequestDTO dto) {
        String normalizedPhone = normalizePhone(dto.getWhatsappPhone());
        if (normalizedPhone == null) {
            logger.warn("WhatsApp preferences rejected: missing whatsappPhone. whatsappEnabled={}",
                    dto != null ? dto.getWhatsappEnabled() : null);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "whatsappPhone e obrigatorio");
        }

        NotificationPreference pref = notificationPreferenceRepository.findByWhatsappPhone(normalizedPhone)
            .orElseGet(NotificationPreference::new);

        pref.setWhatsappEnabled(Boolean.TRUE.equals(dto.getWhatsappEnabled()));
        pref.setWhatsappPhone(normalizedPhone);

        notificationPreferenceRepository.save(pref);
        logger.info("WhatsApp preferences saved: whatsappEnabled={}, phoneSuffix={}",
            pref.getWhatsappEnabled(),
            maskPhone(normalizedPhone));
    }

    public void deletePreferences(String whatsappPhone) {
        String normalizedPhone = normalizePhone(whatsappPhone);
        if (normalizedPhone == null) {
            logger.warn("WhatsApp preferences delete rejected: missing whatsappPhone");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "whatsappPhone e obrigatorio");
        }
        notificationPreferenceRepository.deleteByWhatsappPhone(normalizedPhone);
        logger.info("WhatsApp preferences deleted: phoneSuffix={}", maskPhone(normalizedPhone));
    }

    public String subscribeFromIncomingWhatsapp(String fromAddress) {
        String normalizedPhone = normalizeIncomingWhatsappAddress(fromAddress);
        if (normalizedPhone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro From invalido");
        }

        NotificationPreference pref = notificationPreferenceRepository.findByWhatsappPhone(normalizedPhone)
            .orElseGet(NotificationPreference::new);

        pref.setWhatsappPhone(normalizedPhone);
        pref.setWhatsappEnabled(Boolean.TRUE);

        notificationPreferenceRepository.save(pref);
        logger.info("WhatsApp subscription captured from inbound message: phoneSuffix={}", maskPhone(normalizedPhone));
        return normalizedPhone;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "<empty>";
        }

        if (phone.length() <= 4) {
            return "****" + phone;
        }

        return "****" + phone.substring(phone.length() - 4);
    }

    private NotificationPreferencesResponseDTO toResponse(NotificationPreference pref) {
        return new NotificationPreferencesResponseDTO(
            Boolean.TRUE.equals(pref.getWhatsappEnabled()),
            pref.getWhatsappPhone()
        );
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private String normalizeIncomingWhatsappAddress(String fromAddress) {
        if (fromAddress == null || fromAddress.isBlank()) {
            return null;
        }

        String normalized = fromAddress.trim();
        if (normalized.regionMatches(true, 0, "whatsapp:", 0, 9)) {
            normalized = normalized.substring(9);
        }

        return normalizePhone(normalized);
    }
}
