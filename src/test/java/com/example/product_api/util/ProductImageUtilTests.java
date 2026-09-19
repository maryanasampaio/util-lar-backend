package com.example.product_api.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProductImageUtilTests {

    @Test
    void shouldBuildPublicRefsForDataUrlsAndKeepHttpUrls() {
        List<String> stored = List.of("data:image/png;base64,QUJD", "https://cdn.exemplo.com/foto.jpg");
        List<String> refs = ProductImageUtil.toPublicRefs(7L, stored, Instant.ofEpochMilli(1234L));

        assertEquals(List.of("/produtos/7/imagens/0?v=1234", "https://cdn.exemplo.com/foto.jpg"), refs);
        assertNull(ProductImageUtil.toPublicRefs(7L, null, null));
    }

    @Test
    void shouldDecodeDataUrl() {
        byte[] bytes = "hello".getBytes();
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);

        ProductImageUtil.DecodedImage decoded = ProductImageUtil.decodeDataUrl(dataUrl).orElseThrow();

        assertArrayEquals(bytes, decoded.bytes());
        assertEquals(MediaType.IMAGE_JPEG, decoded.mediaType());
        assertTrue(ProductImageUtil.decodeDataUrl("https://x/y.jpg").isEmpty());
    }

    @Test
    void shouldResolveIncomingRefsBackToStoredContent() {
        List<String> stored = List.of("data:image/png;base64,AAAA", "data:image/png;base64,BBBB");
        List<String> incoming = List.of(
                "http://localhost:8080/produtos/7/imagens/1?v=99",
                "data:image/png;base64,CCCC",
                "/produtos/7/imagens/5",
                "/produtos/8/imagens/0");

        List<String> resolved = ProductImageUtil.resolveIncomingImages(7L, incoming, stored);

        assertEquals(List.of("data:image/png;base64,BBBB", "data:image/png;base64,CCCC"), resolved);
    }
}
