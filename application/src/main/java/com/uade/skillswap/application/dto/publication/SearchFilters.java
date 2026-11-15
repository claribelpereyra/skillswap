package com.uade.skillswap.application.dto.publication;

import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.domain.publication.Modality;

// Filtros del buscador; todos opcionales (null = sin filtro).
public record SearchFilters(String text, Long categoryId, Modality modality, Long minPrice, Long maxPrice,
                            String tag, Double minReputation, SearchSort sort, PageRequest page) {

    public SearchFilters {
        if (minPrice != null && minPrice < 0 || maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("Prices cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("The minimum price cannot be higher than the maximum price");
        }
        if (minReputation != null && (minReputation < 0 || minReputation > 5)) {
            throw new IllegalArgumentException("The minimum reputation must be between 0 and 5");
        }
        sort = sort == null ? SearchSort.RELEVANCE : sort;
        page = page == null ? PageRequest.first() : page;
    }

    public static SearchFilters none() {
        return new SearchFilters(null, null, null, null, null, null, null, null, null);
    }
}
