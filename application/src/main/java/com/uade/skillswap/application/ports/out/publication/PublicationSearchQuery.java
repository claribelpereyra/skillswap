package com.uade.skillswap.application.ports.out.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.in.publication.PublicationSummary;

// Puerto de salida de la búsqueda. Infraestructura lo implementa con una sola consulta paginada
// (proyección con join a la reputación del autor), para no caer en N+1.
public interface PublicationSearchQuery {

    Page<PublicationSummary> search(SearchCriteria criteria, PageRequest page);
}
