package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.in.publication.PublicationSummary;
import com.uade.skillswap.application.ports.in.publication.SearchFilters;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;
import com.uade.skillswap.application.ports.in.publication.SearchSort;
import com.uade.skillswap.controllers.common.Result;
import com.uade.skillswap.domain.publication.Modality;

import static com.uade.skillswap.controllers.common.ControllerSupport.handle;
import static com.uade.skillswap.controllers.common.ControllerSupport.parseEnum;

// BusquedaController del enunciado: buscar, filtrar, ordenar y paginar. No modifica datos,
// así que no necesita validar permisos del usuario activo.
public class SearchController {

    private final SearchPublicationsUseCase search;

    public SearchController(SearchPublicationsUseCase search) {
        this.search = search;
    }

    public Result<Page<PublicationSummary>> search(SearchForm form) {
        return handle(() -> {
            PageRequest page = new PageRequest(form.page() == null ? 0 : form.page(),
                    form.size() == null ? PageRequest.DEFAULT_SIZE : form.size());
            return search.search(new SearchFilters(form.text(), form.categoryId(),
                    parseEnum(Modality.class, form.modality(), "modality"), form.minPrice(), form.maxPrice(),
                    form.skill(), form.minReputation(), parseEnum(SearchSort.class, form.sort(), "sort"), page));
        });
    }
}
