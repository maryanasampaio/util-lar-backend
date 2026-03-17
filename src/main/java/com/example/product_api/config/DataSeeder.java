package com.example.product_api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.product_api.model.User;
import com.example.product_api.model.Product;
import com.example.product_api.model.ProductDimensions;
import com.example.product_api.model.CardBrand;
import com.example.product_api.model.CardFeeRate;
import com.example.product_api.repository.UserRepository;
import com.example.product_api.repository.ProductRepository;
import com.example.product_api.repository.CardBrandRepository;
import com.example.product_api.repository.CardFeeRateRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.math.BigDecimal;

/**
 * Data Seeder - Similar ao Seeder do Laravel
 * Popula o banco com dados iniciais quando a aplicação inicia
 */
@Configuration
@ConditionalOnProperty(value = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            // Verifica se o usuário admin já existe
            if (!userRepository.existsByUsername("admin")) {
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("123456")); // Senha criptografada
                admin.setPermission("ADMIN");
                admin.setCreatedAt(Instant.now());
                admin.setUpdatedAt(Instant.now());
                
                userRepository.save(admin);
                
                System.out.println("✅ Usuário admin criado com sucesso!");
                System.out.println("   Username: admin");
                System.out.println("   Password: 123456");
            } else {
                Optional<User> adminUser = userRepository.findByUsername("admin");
                if (adminUser.isPresent() && !"ADMIN".equalsIgnoreCase(adminUser.get().getPermission())) {
                    User existing = adminUser.get();
                    existing.setPermission("ADMIN");
                    existing.setUpdatedAt(Instant.now());
                    userRepository.save(existing);
                    System.out.println("✅ Permissão do usuário admin atualizada para ADMIN");
                }
                System.out.println("ℹ️  Usuário admin já existe no banco de dados");
            }
        };
    }

    @Bean
    CommandLineRunner initProducts(ProductRepository productRepository) {
        return args -> {
            String seedSlug = "notebook-acer";
            if (!productRepository.existsBySlug(seedSlug)) {
                Product p = new Product();
                p.setName("Notebook Acer Aspire 5");
                p.setSlug(seedSlug);
                p.setDescription("Notebook Acer Aspire 5, Intel i5, 8GB RAM, 256GB SSD");
                p.setPrice(199900L);
                p.setCostPrice(160000L);
                p.setCondition("novo");
                p.setCategory("Eletrodoméstico");
                p.setImages(null);
                p.setStock(5);
                p.setDimensions(new ProductDimensions(36, 2, 24, "cm"));
                p.setMaterial("Plástico e metal");
                p.setColor("Preto");
                p.setBrand("Acer");
                p.setWarranty("90 dias");
                p.setFeatured(true);
                p.setSoldDate(null);
                p.setDisponivel(1);
                p.setCreatedAt(Instant.now());
                p.setUpdatedAt(Instant.now());
                productRepository.save(p);
                System.out.println("✅ Produto seed criado: " + p.getName() + " (slug: " + seedSlug + ")");
            } else {
                System.out.println("ℹ️  Produto seed já existe (slug: " + seedSlug + ")");
            }
        };
    }

    @Bean
    CommandLineRunner initSimulationData(CardBrandRepository cardBrandRepository, CardFeeRateRepository cardFeeRateRepository) {
        return args -> {
            Map<Integer, BigDecimal> rates = new LinkedHashMap<>();
            rates.put(1, new BigDecimal("4.20"));
            rates.put(2, new BigDecimal("6.09"));
            rates.put(3, new BigDecimal("7.01"));
            rates.put(4, new BigDecimal("7.91"));
            rates.put(5, new BigDecimal("8.80"));
            rates.put(6, new BigDecimal("9.67"));
            rates.put(7, new BigDecimal("12.59"));
            rates.put(8, new BigDecimal("13.42"));
            rates.put(9, new BigDecimal("14.25"));
            rates.put(10, new BigDecimal("15.06"));
            rates.put(11, new BigDecimal("15.87"));
            rates.put(12, new BigDecimal("16.66"));

            String[][] brands = {
                {"visa", "Visa"},
                {"mastercard", "Mastercard"},
                {"elo", "Elo"},
                {"amex", "American Express"},
                {"hipercard", "Hipercard"}
            };

            for (String[] b : brands) {
                String brandId = b[0];
                String brandName = b[1];

                CardBrand brand = cardBrandRepository.findById(brandId)
                    .orElseGet(() -> new CardBrand(brandId, brandName, true));

                brand.setName(brandName);
                brand.setActive(true);
                cardBrandRepository.save(brand);

                Map<String, CardFeeRate> existingRates = new HashMap<>();
                for (CardFeeRate existingRate : cardFeeRateRepository.findAllByCardBrand_IdAndActiveTrue(brand.getId())) {
                    existingRates.put(rateKey(existingRate.getInstallments(), existingRate.getRepasse()), existingRate);
                }

                List<CardFeeRate> ratesToSave = new ArrayList<>();

                for (Map.Entry<Integer, BigDecimal> entry : rates.entrySet()) {
                    Integer installments = entry.getKey();
                    BigDecimal ratePercent = entry.getValue();

                    ratesToSave.add(buildRate(existingRates, brand, installments, true, ratePercent));
                    ratesToSave.add(buildRate(existingRates, brand, installments, false, ratePercent));
                }

                cardFeeRateRepository.saveAll(ratesToSave);
            }

            System.out.println("✅ Dados de simulação (bandeiras e taxas) verificados com sucesso");
        };
    }

    private CardFeeRate buildRate(Map<String, CardFeeRate> existingRates, CardBrand brand, Integer installments, Boolean repasse, BigDecimal ratePercent) {
        CardFeeRate rate = existingRates.get(rateKey(installments, repasse));
        if (rate == null) {
            rate = new CardFeeRate();
            rate.setCardBrand(brand);
            rate.setInstallments(installments);
            rate.setRepasse(repasse);
        }

        rate.setRatePercent(ratePercent);
        rate.setActive(true);
        return rate;
    }

    private String rateKey(Integer installments, Boolean repasse) {
        return installments + ":" + repasse;
    }
}
