package com.example.product_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.product_api.model.NotificationPreference;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByWhatsappPhone(String whatsappPhone);

    void deleteByWhatsappPhone(String whatsappPhone);

    @Query("""
        SELECT np.whatsappPhone
        FROM NotificationPreference np
        WHERE np.whatsappEnabled = true
          AND np.whatsappPhone IS NOT NULL
          AND np.whatsappPhone <> ''
    """)
    List<String> findAllActiveWhatsappPhones();
}
