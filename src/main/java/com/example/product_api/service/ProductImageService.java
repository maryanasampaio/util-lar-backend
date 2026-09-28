package com.example.product_api.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class ProductImageService {

    private static final Logger logger = LoggerFactory.getLogger(ProductImageService.class);

    public static final String IMAGE_PATH = "/api/products/images/";
    private static final String LEGACY_IMAGE_PATH = "/produtos/images/";
    private static final String KEY_PREFIX = "produtos/";
    private static final String DATA_URL_PREFIX = "data:";

    private static final Map<String, String> EXTENSION_BY_MIME = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/svg+xml", "svg",
            "image/bmp", "bmp",
            "image/avif", "avif"
    );

    private final StorageService storage;
    private final String configuredBaseUrl;
    private volatile String lastKnownBaseUrl = "";

    public ProductImageService(StorageService storage, @Value("${app.api-base-url:}") String apiBaseUrl) {
        this.storage = storage;
        this.configuredBaseUrl = stripTrailingSlash(apiBaseUrl);
    }

    private String resolveBaseUrl() {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return configuredBaseUrl;
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            String fromRequest = stripTrailingSlash(
                    ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
            lastKnownBaseUrl = fromRequest;
            return fromRequest;
        }
        return lastKnownBaseUrl;
    }

    public List<String> normalizeForStorage(Long productId, List<String> images) {
        if (images == null) {
            return null;
        }
        if (!storage.isConfigured()) {
            logger.warn("Storage S3 nao configurado: imagens do produto {} serao gravadas no formato "
                    + "anterior (base64 no banco). Defina S3_URL, S3_BUCKET, S3_KEY_ID e S3_SECRET_KEY.",
                    productId);
            return images;
        }
        List<String> result = new ArrayList<>(images.size());
        for (String image : images) {
            if (image == null || image.isBlank()) {
                continue;
            }
            String value = image.trim();
            if (isDataUrl(value)) {
                result.add(uploadDataUrl(productId, value));
            } else if (isOwnUrl(value)) {
                result.add(extractKey(value));
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private String uploadDataUrl(Long productId, String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Imagem em data URL sem conteudo");
        }
        String header = dataUrl.substring(DATA_URL_PREFIX.length(), comma);
        String mime = header.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (mime.isEmpty()) {
            mime = "application/octet-stream";
        }
        if (!header.toLowerCase(Locale.ROOT).contains("base64")) {
            throw new IllegalArgumentException("Imagem em data URL nao esta em base64");
        }

        byte[] bytes = Base64.getMimeDecoder().decode(dataUrl.substring(comma + 1));
        String ext = EXTENSION_BY_MIME.getOrDefault(mime, "bin");
        String key = KEY_PREFIX + productId + "/" + UUID.randomUUID() + "." + ext;

        long startedAt = System.currentTimeMillis();
        try {
            storage.upload(key, bytes, mime);
        } catch (RuntimeException ex) {
            logger.error("Product image upload FAILED: productId={}, key={}, bytes={}, contentType={}, target={}, elapsedMs={}, reason={}",
                    productId, key, bytes.length, mime, storage.describeTarget(),
                    System.currentTimeMillis() - startedAt, ex.getMessage());
            throw ex;
        }
        logger.info("Product image uploaded: productId={}, key={}, bytes={}, contentType={}, elapsedMs={}",
                productId, key, bytes.length, mime, System.currentTimeMillis() - startedAt);
        return key;
    }

    public String toPublicUrl(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String value = stored.trim();
        if (isDataUrl(value) || isExternalUrl(value)) {
            return value;
        }
        return resolveBaseUrl() + IMAGE_PATH + value;
    }

    public List<String> toPublicUrls(List<String> stored) {
        if (stored == null) {
            return null;
        }
        List<String> result = new ArrayList<>(stored.size());
        for (String item : stored) {
            String url = toPublicUrl(item);
            if (url != null) {
                result.add(url);
            }
        }
        return result;
    }

    public void deleteRemoved(List<String> before, List<String> after) {
        if (before == null || before.isEmpty()) {
            return;
        }
        Set<String> kept = new HashSet<>();
        if (after != null) {
            after.forEach(item -> kept.add(item != null ? item.trim() : null));
        }
        for (String item : before) {
            if (item == null) {
                continue;
            }
            String key = item.trim();
            if (isKey(key) && !kept.contains(key)) {
                safeDelete(key);
            }
        }
    }

    public void deleteAll(Long productId) {
        String prefix = KEY_PREFIX + productId + "/";
        try {
            for (String key : storage.list(prefix)) {
                safeDelete(key);
            }
        } catch (RuntimeException ex) {
            logger.warn("Failed to list product images for deletion: prefix={}, reason={}", prefix, ex.getMessage());
        }
    }

    private void safeDelete(String key) {
        try {
            storage.delete(key);
            logger.info("Product image deleted: key={}", key);
        } catch (RuntimeException ex) {
            logger.warn("Failed to delete product image: key={}, reason={}", key, ex.getMessage());
        }
    }

    public boolean isDataUrl(String value) {
        return value != null && value.regionMatches(true, 0, DATA_URL_PREFIX, 0, DATA_URL_PREFIX.length());
    }

    public boolean isExternalUrl(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return (lower.startsWith("http://") || lower.startsWith("https://")) && !isOwnUrl(value);
    }

    public boolean isOwnUrl(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        return value.contains(IMAGE_PATH) || value.contains(LEGACY_IMAGE_PATH);
    }

    public boolean isKey(String value) {
        return value != null && value.startsWith(KEY_PREFIX);
    }

    public boolean isStorageConfigured() {
        return storage.isConfigured();
    }

    public boolean hasLegacyImage(List<String> images) {
        return images != null && images.stream().anyMatch(this::isDataUrl);
    }

    private String extractKey(String ownUrl) {
        int idx = ownUrl.indexOf(IMAGE_PATH);
        int len = IMAGE_PATH.length();
        if (idx < 0) {
            idx = ownUrl.indexOf(LEGACY_IMAGE_PATH);
            len = LEGACY_IMAGE_PATH.length();
        }
        String key = ownUrl.substring(idx + len);
        int query = key.indexOf('?');
        return query >= 0 ? key.substring(0, query) : key;
    }

    private static String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
