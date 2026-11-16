package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.Objects;
import java.util.function.BiConsumer;

// Adapter: reserva y libera franjas usando la Publication, que es la que cuenta el cupo.
public class PublicationAvailabilityAdapter implements PublicationAvailability {

    private final PublicationRepository publications;

    public PublicationAvailabilityAdapter(PublicationRepository publications) {
        this.publications = Objects.requireNonNull(publications);
    }

    @Override
    public void reserve(long publicationId, long slotId) {
        change(publicationId, slotId, Publication::reserve);
    }

    @Override
    public void release(long publicationId, long slotId) {
        change(publicationId, slotId, Publication::release);
    }

    private void change(long publicationId, long slotId, BiConsumer<Publication, TimeSlot> action) {
        Publication publication = publications.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication", publicationId));
        TimeSlot slot = publication.getSlots().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), slotId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("The slot does not belong to the publication"));
        action.accept(publication, slot);
        publications.save(publication);
    }
}
