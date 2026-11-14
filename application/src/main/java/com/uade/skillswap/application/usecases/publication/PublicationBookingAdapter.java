package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.Optional;

// Lo que contratación necesita de publicaciones (los puertos PublicationCatalog y PublicationAvailability),
// resuelto con el agregado Publication y su repositorio. Las reglas del cupo y las franjas las aplica el
// dominio (Publication.reserve/release); acá solo se busca, se delega y se guarda.
//
// No abre su propia transacción: corre dentro de la unidad de trabajo del caso de uso de contratación, así
// aceptar la contratación y ocupar el lugar se confirman juntos. Si dos aceptaciones compiten por el último
// lugar, el optimistic locking de la publicación hace fallar la segunda al confirmar (caso de aceptación 4).
public class PublicationBookingAdapter implements PublicationCatalog, PublicationAvailability {

    private final PublicationRepository publications;

    public PublicationBookingAdapter(PublicationRepository publications) {
        this.publications = publications;
    }

    @Override
    public Optional<BookablePublication> findBookable(long publicationId) {
        return publications.findById(publicationId).map(publication -> new BookablePublication(
                publication.getId(),
                publication.getAuthorId(),
                publication.acceptsRequests(),
                publication.getDetails().basePrice(),
                publication.getSlots()));
    }

    @Override
    public void reserve(long publicationId, long slotId) {
        Publication publication = load(publicationId);
        publication.reserve(slot(publication, slotId));
        publications.save(publication);
    }

    @Override
    public void release(long publicationId, long slotId) {
        Publication publication = load(publicationId);
        publication.release(slot(publication, slotId));
        publications.save(publication);
    }

    private Publication load(long publicationId) {
        return publications.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication", publicationId));
    }

    private static TimeSlot slot(Publication publication, long slotId) {
        return publication.getSlots().stream()
                .filter(slot -> slot.getId() != null && slot.getId() == slotId)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("The slot does not belong to the publication"));
    }
}
