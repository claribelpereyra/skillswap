package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;
import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.application.ports.out.publication.CategoryTree;
import com.uade.skillswap.application.ports.out.publication.PublicationSearchQuery;
import com.uade.skillswap.application.ports.out.publication.SearchCriteria;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class SearchPublicationsService implements SearchPublicationsUseCase {

    private final PublicationSearchQuery searchQuery;
    private final CategoryTree categories;

    public SearchPublicationsService(PublicationSearchQuery searchQuery, CategoryTree categories) {
        this.searchQuery = Objects.requireNonNull(searchQuery);
        this.categories = Objects.requireNonNull(categories);
    }

    // Normaliza los filtros y delega; la consulta filtra y ordena en la base para paginar.
    @Override
    public Page<PublicationSummary> search(SearchFilters filters) {
        String text = normalize(filters.text());
        SearchSort sort = filters.sort() == SearchSort.RELEVANCE && text == null
                ? SearchSort.BEST_REPUTATION
                : filters.sort();
        SearchCriteria criteria = new SearchCriteria(text, categoryIdsFor(filters.categoryId()), filters.modality(),
                filters.minPrice(), filters.maxPrice(), normalize(filters.tag()), filters.minReputation(), sort);
        return searchQuery.search(criteria, filters.page());
    }

    private Set<Long> categoryIdsFor(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categories.idsUnder(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
