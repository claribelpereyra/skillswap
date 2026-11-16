package com.uade.skillswap.infrastructure.persistence.profile;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.ports.out.profile.ActivePublicationsQuery;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.PublicationStatus;

import java.util.Objects;

public class JpaActivePublicationsQuery implements ActivePublicationsQuery {

    private final PublicationRepository publications;

    public JpaActivePublicationsQuery(PublicationRepository publications) {
        this.publications = Objects.requireNonNull(publications, "publications must not be null");
    }

    @Override
    public Page<PublicationView> findActiveByUserId(UserId userId, PageRequest page) {
        PageRequest safePage = page != null ? page : PageRequest.first();
        if (userId == null) {
            return Page.empty(safePage);
        }
        return publications.findByAuthor(userId, PublicationStatus.PUBLISHED, safePage)
                .map(PublicationView::from);
    }
}
