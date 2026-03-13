package com.example.product_api.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.product_api.dto.NotificationDTO.NotificationPreferencesRequestDTO;
import com.example.product_api.dto.NotificationDTO.NotificationPreferencesResponseDTO;
import com.example.product_api.model.NotificationPreference;
import com.example.product_api.repository.NotificationPreferenceRepository;

@Service
public class NotificationPreferenceService {

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "whatsappPhone e obrigatorio");
        }

        NotificationPreference pref = notificationPreferenceRepository.findByWhatsappPhone(normalizedPhone)
            .orElseGet(NotificationPreference::new);

        pref.setWhatsappEnabled(Boolean.TRUE.equals(dto.getWhatsappEnabled()));
        pref.setWhatsappPhone(normalizedPhone);

        notificationPreferenceRepository.save(pref);
    }

    public void deletePreferences(String whatsappPhone) {
        String normalizedPhone = normalizePhone(whatsappPhone);
        if (normalizedPhone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "whatsappPhone e obrigatorio");
        }
        notificationPreferenceRepository.deleteByWhatsappPhone(normalizedPhone);
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
        return normalizedPhone;
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
