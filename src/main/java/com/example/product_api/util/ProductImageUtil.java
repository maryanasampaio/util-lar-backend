package com.example.product_api.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;

public final class ProductImageUtil {

    public static final String IMAGE_PATH_TEMPLATE = "/produtos/%d/imagens/%d";

    private static final Pattern DATA_URL = Pattern.compile(
            "^data:([a-zA-Z0-9.+/-]+)?(;[^,]*)?,(.*)$", Pattern.DOTALL);
    private static final Pattern IMAGE_REF = Pattern.compile(
            "^(?:https?://[^/]+)?(?:/[^/?]+)*?/produtos/(\\d+)/imagens/(\\d+)(?:\\?.*)?$");

    private ProductImageUtil() {
    }

    public record DecodedImage(byte[] bytes, MediaType mediaType) {
    }

    public static String imageRef(Long productId, int index, Instant version) {
        long v = version != null ? version.toEpochMilli() : 0L;
        return String.format(IMAGE_PATH_TEMPLATE, productId, index) + "?v=" + v;
    }

    public static List<String> toPublicRefs(Long productId, List<String> stored, Instant version) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        List<String> refs = new ArrayList<>(stored.size());
        for (int i = 0; i < stored.size(); i++) {
            String value = stored.get(i);
            if (isHttpUrl(value)) {
                refs.add(value.trim());
            } else {
                refs.add(imageRef(productId, i, version));
            }
        }
        return refs;
    }

    public static List<String> resolveIncomingImages(Long productId, List<String> incoming, List<String> stored) {
        if (incoming == null) {
            return null;
        }
        List<String> resolved = new ArrayList<>(incoming.size());
        for (String value : incoming) {
            Optional<int[]> ref = parseImageRef(value);
            if (ref.isPresent() && productId != null && ref.get()[0] == productId
                    && stored != null && ref.get()[1] < stored.size()) {
                resolved.add(stored.get(ref.get()[1]));
            } else if (ref.isPresent()) {
                continue;
            } else {
                resolved.add(value);
            }
        }
        return resolved;
    }

    public static Optional<int[]> parseImageRef(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher m = IMAGE_REF.matcher(value.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) });
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static boolean isHttpUrl(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return v.startsWith("http://") || v.startsWith("https://");
    }

    public static boolean isDataUrl(String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).startsWith("data:");
    }

    public static Optional<DecodedImage> decodeDataUrl(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher m = DATA_URL.matcher(value.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        String mime = m.group(1) != null ? m.group(1) : "application/octet-stream";
        String params = m.group(2) != null ? m.group(2).toLowerCase(Locale.ROOT) : "";
        String payload = m.group(3);
        try {
            byte[] bytes = params.contains("base64")
                    ? Base64.getMimeDecoder().decode(payload)
                    : java.net.URLDecoder.decode(payload, java.nio.charset.StandardCharsets.UTF_8)
                            .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            MediaType type;
            try {
                type = MediaType.parseMediaType(mime);
            } catch (Exception e) {
                type = MediaType.APPLICATION_OCTET_STREAM;
            }
            return Optional.of(new DecodedImage(bytes, type));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
