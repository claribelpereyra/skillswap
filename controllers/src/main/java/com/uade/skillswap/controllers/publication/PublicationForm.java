package com.uade.skillswap.controllers.publication;

import java.time.Instant;
import java.util.List;
import java.util.Set;

// Lo que carga el autor en el formulario de publicación, con tipos simples como los entrega la UI.
// type: MENTORING, SERVICE o CHALLENGE. modality: REMOTE u ON_SITE.
public record PublicationForm(String type, String title, String description, Long categoryId, String modality,
                              Integer durationMinutes, int capacity, Long basePrice, Set<String> tags,
                              List<SlotForm> slots) {

    public record SlotForm(Instant startsAt, Instant endsAt) {
    }
}
