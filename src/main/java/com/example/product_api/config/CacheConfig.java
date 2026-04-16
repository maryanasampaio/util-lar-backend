package com.example.product_api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configuração do gerenciador de cache usando Caffeine
     * 
     * Caches configurados:
     * - dashboard: 1 minuto TTL (métricas do dashboard)
     * - monthlyEvolution: 1 minuto TTL (evolução mensal)
     * - products: 1 minuto TTL (produtos por tipo)
     * - soldProductsByMonth: 1 minuto TTL (vendidos por mês)
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "dashboard", 
            "monthlyEvolution", 
            "products",
            "soldProductsByMonth"
        );
        
        // Configuração padrão: 1 minuto de TTL, máximo 200 entradas
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(200)
            .recordStats());
        
        return cacheManager;
    }

}
