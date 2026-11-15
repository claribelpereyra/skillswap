package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.domain.publication.Modality;

// Lo que el usuario completa en el buscador. Todos los filtros son opcionales (null = sin filtro).
// - categoryId: incluye sus subcategorías y habilidades.
// - skill: nombre de una habilidad; matchea contra las etiquetas de la publicación.
// - minReputation: reputación mínima del autor, de 0 a 5.
public record SearchFilters(String text, Long categoryId, Modality modality, Long minPrice, Long maxPrice,
                            String skill, Double minReputation, SearchSort sort, PageRequest page) {

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
