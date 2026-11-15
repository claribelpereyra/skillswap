package com.uade.skillswap.domain.publication;

import com.uade.skillswap.domain.common.SkillCoins;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// Datos editables de una publicación; lo obligatorio se exige recién al publicar.
public record PublicationDetails(String title, String description, Long categoryId, Modality modality,
                                 Duration estimatedDuration, int capacity, SkillCoins basePrice,
                                 Set<String> tags) {

    public static final int MAX_TITLE = 150;
    public static final int MAX_DESCRIPTION = 2000;
    public static final int MAX_TAG = 50;

    public PublicationDetails {
        if (capacity < 1) {
            throw new IllegalArgumentException("The capacity must be at least 1: " + capacity);
        }
        if (estimatedDuration != null && (estimatedDuration.isNegative() || estimatedDuration.isZero())) {
            throw new IllegalArgumentException("The estimated duration must be positive");
        }
        requireMaxLength(title, MAX_TITLE, "title");
        requireMaxLength(description, MAX_DESCRIPTION, "description");
        tags = normalize(tags);
        tags.forEach(tag -> requireMaxLength(tag, MAX_TAG, "tag"));
    }

    // Etiquetas sin espacios y en minúsculas: "Java" y "java" son la misma.
    private static Set<String> normalize(Set<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(tag -> tag.strip().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void requireMaxLength(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException("The " + field + " cannot be longer than " + max + " characters");
        }
    }
}
