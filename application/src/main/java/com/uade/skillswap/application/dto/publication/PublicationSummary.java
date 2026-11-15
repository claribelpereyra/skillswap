package com.uade.skillswap.application.dto.publication;

import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationType;

import java.time.Instant;
import java.util.Set;

// Fila del resultado de búsqueda: proyección con solo lo que muestra la lista (evita N+1).
public record PublicationSummary(long id, String authorId, double authorReputation, PublicationType type,
                                 String title, Long categoryId, Modality modality, long basePrice,
                                 int availableSpots, Set<String> tags, Instant publishedAt) {

    public PublicationSummary {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
