package com.uade.skillswap.application.dto.publication;

import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;

import java.time.Instant;
import java.util.List;
import java.util.Set;

// Lo que devuelven los casos de uso de publicación: datos planos para los controllers y la UI,
// sin exponer el agregado (así nadie de afuera llama a sus métodos de negocio).
public record PublicationView(Long id, String authorId, PublicationType type, PublicationStatus status,
                              String title, String description, Long categoryId, Modality modality,
                              Long durationMinutes, int capacity, int availableSpots, Long basePrice,
                              Set<String> tags, List<SlotView> slots) {

    public record SlotView(Long id, Instant startsAt, Instant endsAt, boolean occupied) {
    }

    public static PublicationView from(Publication publication) {
        PublicationDetails details = publication.getDetails();
        return new PublicationView(
                publication.getId(),
                publication.getAuthorId().value(),
                publication.getType(),
                publication.getStatus(),
                details.title(),
                details.description(),
                details.categoryId(),
                details.modality(),
                details.estimatedDuration() == null ? null : details.estimatedDuration().toMinutes(),
                details.capacity(),
                publication.availableSpots(),
                details.basePrice() == null ? null : details.basePrice().amount(),
                details.tags(),
                publication.getSlots().stream()
                        .map(s -> new SlotView(s.getId(), s.getStartsAt(), s.getEndsAt(), s.isOccupied()))
                        .toList());
    }
}
