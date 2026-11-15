package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.common.Page;

public interface RecommendPublicationsUseCase {

    // La estrategia puede cambiarse en cada llamada sin modificar el caso de uso.
    Page<PublicationSummary> recommend(SearchFilters filters, RecommendationStrategy strategy);
}
