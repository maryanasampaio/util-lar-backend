package com.example.product_api.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.example.product_api.config.S3Config;

@EnabledIfEnvironmentVariable(named = "S3_IT", matches = "true")
class StorageServiceIT {

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value != null && !value.isBlank() ? value : fallback;
    }

    @Test
    void uploadDownloadListDeleteRoundTrip() {
        String url = env("S3_URL", "");
        String keyId = env("S3_KEY_ID", "");
        String secretKey = env("S3_SECRET_KEY", "");
        String bucket = env("S3_BUCKET", "util-lar");

        StorageService storage = new StorageService(
                S3Config.build(url, keyId, secretKey, env("S3_REGION", "garage")),
                bucket, url, keyId, secretKey);

        String prefix = "produtos/it-" + UUID.randomUUID() + "/";
        String key = prefix + "teste.txt";
        byte[] bytes = "round-trip do storage".getBytes(StandardCharsets.UTF_8);

        assertFalse(storage.exists(key));

        storage.upload(key, bytes, "text/plain");
        assertTrue(storage.exists(key));

        StorageService.StoredFile file = storage.download(key).orElseThrow();
        assertArrayEquals(bytes, file.bytes());
        assertEquals("text/plain", file.contentType());

        List<String> listed = storage.list(prefix);
        assertEquals(List.of(key), listed);

        storage.delete(key);
        assertFalse(storage.exists(key));
        assertTrue(storage.download(key).isEmpty());
        assertTrue(storage.list(prefix).isEmpty());
    }
}
