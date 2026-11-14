package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.ports.in.publication.PublicationSummary;
import com.uade.skillswap.controllers.common.Result;

// BusquedaController del enunciado: buscar, filtrar, ordenar y paginar.
public interface SearchController {

    Result<Page<PublicationSummary>> search(SearchForm form);
}
