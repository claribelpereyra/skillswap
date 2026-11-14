package com.uade.skillswap.domain.publication;

import com.uade.skillswap.domain.common.SkillCoins;

import java.time.Duration;
import java.util.Set;

// Datos editables de una publicación. En borrador pueden estar incompletos: lo obligatorio
// se exige recién al publicar (ver Publication#publish).
public record PublicationDetails(String title, String description, Long categoryId, Modality modality,
                                 Duration estimatedDuration, int capacity, SkillCoins basePrice,
                                 Set<String> tags) {

    public PublicationDetails {
        if (capacity < 1) {
            throw new IllegalArgumentException("The capacity must be at least 1: " + capacity);
        }
        if (estimatedDuration != null && (estimatedDuration.isNegative() || estimatedDuration.isZero())) {
            throw new IllegalArgumentException("The estimated duration must be positive");
        }
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
