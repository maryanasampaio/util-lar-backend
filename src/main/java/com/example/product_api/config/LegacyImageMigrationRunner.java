package com.example.product_api.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.product_api.service.ProductService;

@Component
@ConditionalOnProperty(name = "storage.migrate-on-startup", havingValue = "true")
public class LegacyImageMigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(LegacyImageMigrationRunner.class);

    private final ProductService productService;

    public LegacyImageMigrationRunner(ProductService productService) {
        this.productService = productService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void migrateOnStartup() {
        logger.info("Legacy image migration on startup: started");
        try {
            Map<String, Object> report = productService.migrateLegacyImages(null);
            logger.info("Legacy image migration on startup: finished {}", report);
        } catch (RuntimeException ex) {
            logger.error("Legacy image migration on startup: failed ({})", ex.getMessage(), ex);
        }
    }
}
