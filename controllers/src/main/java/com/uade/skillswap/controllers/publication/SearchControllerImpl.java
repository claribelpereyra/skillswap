package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.common.ControllerSupport.InvalidInputException;
import com.uade.skillswap.domain.publication.Modality;

import java.util.Objects;

import static com.uade.skillswap.controllers.common.ControllerSupport.parseEnum;

public class SearchControllerImpl implements SearchController {

    private final SearchPublicationsUseCase search;

    public SearchControllerImpl(SearchPublicationsUseCase search) {
        this.search = Objects.requireNonNull(search);
    }

    @Override
    public ControllerResult<Page<PublicationSummary>> search(SearchForm form) {
        return ControllerResult.of(() -> {
            if (form == null) {
                throw new InvalidInputException("The search form is required");
            }
            PageRequest page = new PageRequest(form.page() == null ? 0 : form.page(),
                    form.size() == null ? PageRequest.DEFAULT_SIZE : form.size());
            return search.search(new SearchFilters(form.text(), form.categoryId(),
                    parseEnum(Modality.class, form.modality(), "modality"), form.minPrice(), form.maxPrice(),
                    form.tag(), form.minReputation(), parseEnum(SearchSort.class, form.sort(), "sort"), page));
        });
    }
}
