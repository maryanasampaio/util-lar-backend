package com.example.product_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTests {

    private static final String BASE = "http://localhost:8080";
    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, 3};
    private static final String PNG_DATA_URL = "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG_BYTES);

    @Mock
    private StorageService storage;

    private ProductImageService service;

    @BeforeEach
    void setUp() {
        lenient().when(storage.isConfigured()).thenReturn(true);
        service = new ProductImageService(storage, BASE + "/");
    }

    @Test
    void shouldKeepBase64WhenStorageIsNotConfigured() {
        when(storage.isConfigured()).thenReturn(false);

        List<String> stored = service.normalizeForStorage(12L, List.of(PNG_DATA_URL));

        assertEquals(List.of(PNG_DATA_URL), stored);
        verify(storage, never()).upload(any(), any(), any());
    }

    @Test
    void shouldUploadDataUrlAndReturnKey() {
        when(storage.upload(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        List<String> stored = service.normalizeForStorage(12L, List.of(PNG_DATA_URL));

        assertEquals(1, stored.size());
        assertTrue(stored.get(0).startsWith("produtos/12/"));
        assertTrue(stored.get(0).endsWith(".png"));

        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(storage).upload(startsWith("produtos/12/"), bytes.capture(), eq("image/png"));
        assertEquals(PNG_BYTES.length, bytes.getValue().length);
    }

    @Test
    void shouldExtractKeyWhenFrontSendsBackOwnUrl() {
        List<String> stored = service.normalizeForStorage(12L, List.of(
                BASE + "/api/products/images/produtos/12/abc.png",
                "https://api.util-lar.com/util-lar/api/products/images/produtos/12/def.jpg?x=1"));

        assertEquals(List.of("produtos/12/abc.png", "produtos/12/def.jpg"), stored);
        verify(storage, never()).upload(any(), any(), any());
    }

    @Test
    void shouldKeepExternalUrlsAndKeysUntouched() {
        List<String> stored = service.normalizeForStorage(12L, List.of(
                "https://cdn.exemplo.com/mesa.jpg",
                "produtos/12/ja-e-key.png",
                "  "));

        assertEquals(List.of("https://cdn.exemplo.com/mesa.jpg", "produtos/12/ja-e-key.png"), stored);
    }

    @Test
    void shouldBuildPublicUrlFromKeyAndPassThroughOthers() {
        assertEquals(BASE + "/api/products/images/produtos/12/abc.png", service.toPublicUrl("produtos/12/abc.png"));
        assertEquals("https://cdn.exemplo.com/mesa.jpg", service.toPublicUrl("https://cdn.exemplo.com/mesa.jpg"));
        assertEquals(PNG_DATA_URL, service.toPublicUrl(PNG_DATA_URL));
    }

    @Test
    void shouldDeleteOnlyRemovedKeys() {
        service.deleteRemoved(
                List.of("produtos/12/a.png", "produtos/12/b.png", "https://cdn.exemplo.com/x.jpg"),
                List.of("produtos/12/b.png"));

        verify(storage).delete("produtos/12/a.png");
        verify(storage, never()).delete("produtos/12/b.png");
        verify(storage, never()).delete("https://cdn.exemplo.com/x.jpg");
    }

    @Test
    void shouldDetectLegacyImages() {
        assertTrue(service.hasLegacyImage(List.of("produtos/1/a.png", PNG_DATA_URL)));
        assertFalse(service.hasLegacyImage(List.of("produtos/1/a.png")));
        assertFalse(service.hasLegacyImage(null));
    }
}
