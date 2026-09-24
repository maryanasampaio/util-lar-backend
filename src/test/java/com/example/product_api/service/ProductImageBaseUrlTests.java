package com.example.product_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class ProductImageBaseUrlTests {

    private static final String KEY = "produtos/7/abc.png";

    @Mock
    private StorageService storage;

    @BeforeEach
    void setUp() {
        lenient().when(storage.isConfigured()).thenReturn(true);
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void bindRequest(String scheme, String host, int port, String forwardedPrefix) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName(host);
        request.setServerPort(port);
        if (forwardedPrefix != null) {
            request.setContextPath(forwardedPrefix);
            request.setRequestURI(forwardedPrefix + "/api/products");
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void shouldUseConfiguredBaseUrlWhenProvided() {
        ProductImageService service = new ProductImageService(storage, "https://api.util-lar.com/util-lar/");
        bindRequest("http", "qualquer-host", 8080, null);

        assertEquals("https://api.util-lar.com/util-lar/api/products/images/" + KEY, service.toPublicUrl(KEY));
    }

    @Test
    void shouldDeriveBaseUrlFromRequestWhenNotConfigured() {
        ProductImageService service = new ProductImageService(storage, "");
        bindRequest("https", "api-hmg.util-lar.com", 443, null);

        assertEquals("https://api-hmg.util-lar.com/api/products/images/" + KEY, service.toPublicUrl(KEY));
    }

    @Test
    void shouldKeepProxyPrefixWhenDerivingFromRequest() {
        ProductImageService service = new ProductImageService(storage, "");
        bindRequest("https", "api-hmg.util-lar.com", 443, "/util-lar");

        assertEquals("https://api-hmg.util-lar.com/util-lar/api/products/images/" + KEY, service.toPublicUrl(KEY));
    }

    @Test
    void shouldReuseLastKnownBaseUrlOutsideOfRequest() {
        ProductImageService service = new ProductImageService(storage, "");
        bindRequest("https", "api-hmg.util-lar.com", 443, "/util-lar");
        service.toPublicUrl(KEY);

        RequestContextHolder.resetRequestAttributes();

        assertEquals("https://api-hmg.util-lar.com/util-lar/api/products/images/" + KEY, service.toPublicUrl(KEY));
    }
}
