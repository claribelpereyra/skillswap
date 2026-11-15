package com.uade.skillswap.application.ports.out.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationSummary;

// Puerto de salida de la búsqueda: filtra, ordena y pagina en la base.
public interface PublicationSearchQuery {

    Page<PublicationSummary> search(SearchCriteria criteria, PageRequest page);
}
