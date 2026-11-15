package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationType;

import java.time.Instant;
import java.util.Set;

// Una fila del resultado de búsqueda. Es una proyección: trae solo lo que muestra la lista,
// sin cargar franjas ni historial de cada publicación (evita N+1).
public record PublicationSummary(long id, String authorId, double authorReputation, PublicationType type,
                                 String title, Long categoryId, Modality modality, long basePrice,
                                 int availableSpots, Set<String> tags, Instant publishedAt) {

    public PublicationSummary {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
