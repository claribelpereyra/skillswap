package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.publication.Publication;

import java.util.Objects;
import java.util.Optional;

// Adapter: traduce la Publication del área de publicaciones a lo que necesita contratación.
public class PublicationCatalogAdapter implements PublicationCatalog {

    private final PublicationRepository publications;

    public PublicationCatalogAdapter(PublicationRepository publications) {
        this.publications = Objects.requireNonNull(publications);
    }

    @Override
    public Optional<BookablePublication> findBookable(long publicationId) {
        return publications.findById(publicationId).map(PublicationCatalogAdapter::toBookable);
    }

    private static BookablePublication toBookable(Publication publication) {
        return new BookablePublication(
                publication.getId(),
                publication.getAuthorId(),
                publication.acceptsRequests(),
                publication.getDetails().basePrice(),
                publication.getSlots());
    }
}
