package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.domain.common.UserId;

public interface RecommendPublicationsUseCase {

    // La estrategia puede cambiarse en cada llamada sin modificar el caso de uso.
    Page<PublicationSummary> recommend(SearchFilters filters, UserId activeUserId, RecommendationStrategy strategy);
}
