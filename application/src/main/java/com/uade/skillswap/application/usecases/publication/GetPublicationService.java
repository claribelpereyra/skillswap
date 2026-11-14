package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.publication.GetPublicationUseCase;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public class GetPublicationService implements GetPublicationUseCase {

    private final PublicationRepository publications;

    public GetPublicationService(PublicationRepository publications) {
        this.publications = Objects.requireNonNull(publications);
    }

    // Un borrador o una pausada ajena responde "no encontrada", igual que una que no existe:
    // así no se filtra que existe algo que el usuario no debería ver.
    @Override
    public PublicationView get(UserId viewerId, long publicationId) {
        return publications.findById(publicationId)
                .filter(publication -> publication.isVisibleTo(viewerId))
                .map(PublicationView::from)
                .orElseThrow(() -> new ResourceNotFoundException("Publication", publicationId));
    }
}
