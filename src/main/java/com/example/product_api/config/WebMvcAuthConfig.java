package com.example.product_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcAuthConfig implements WebMvcConfigurer {

    private final AuthTokenInterceptor authTokenInterceptor;
    private final FinancialAuthInterceptor financialAuthInterceptor;

    public WebMvcAuthConfig(AuthTokenInterceptor authTokenInterceptor, 
                           FinancialAuthInterceptor financialAuthInterceptor) {
        this.authTokenInterceptor = authTokenInterceptor;
        this.financialAuthInterceptor = financialAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor para gerenciamento de produtos (POST/PUT/PATCH/DELETE)
        registry.addInterceptor(authTokenInterceptor)
                .addPathPatterns("/api/products/**", "/produtos/**", "/api/products", "/produtos");
        
        // Interceptor para endpoints financeiros (TODOS os métodos HTTP)
        registry.addInterceptor(financialAuthInterceptor)
                .addPathPatterns("/api/financeiro/**", "/financeiro/**");
    }
}
