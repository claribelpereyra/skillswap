package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.in.publication.ListMyPublicationsUseCase;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.PublicationStatus;

import java.util.Objects;

public class ListMyPublicationsService implements ListMyPublicationsUseCase {

    private final PublicationRepository publications;

    public ListMyPublicationsService(PublicationRepository publications) {
        this.publications = Objects.requireNonNull(publications);
    }

    @Override
    public Page<PublicationView> listMine(UserId authorId, PublicationStatus status, PageRequest page) {
        Objects.requireNonNull(authorId, "The author is required");
        return publications.findByAuthor(authorId, status, page == null ? PageRequest.first() : page)
                .map(PublicationView::from);
    }
}
