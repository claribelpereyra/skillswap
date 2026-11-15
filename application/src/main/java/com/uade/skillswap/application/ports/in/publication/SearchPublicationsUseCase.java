package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.common.Page;

public interface SearchPublicationsUseCase {

    // Busca entre las publicaciones PUBLICADAS, filtra, ordena y pagina.
    Page<PublicationSummary> search(SearchFilters filters);
}
