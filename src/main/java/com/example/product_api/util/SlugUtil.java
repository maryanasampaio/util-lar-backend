package com.example.product_api.util;

import java.text.Normalizer;

public final class SlugUtil {
    private SlugUtil() {}

    public static String slugify(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String lower = withoutAccents.toLowerCase();
        String hyphenated = lower.replaceAll("[^a-z0-9]+", "-");
        String cleaned = hyphenated.replaceAll("-+", "-");
        return cleaned.replaceAll("^-|-$", "");
    }
}
