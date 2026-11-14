package com.uade.skillswap.controllers.publication;

// Lo que completa el usuario en el buscador. Todo es opcional; page arranca en 0.
// sort: RELEVANCE, BEST_REPUTATION, LOWEST_PRICE o MOST_RECENT.
public record SearchForm(String text, Long categoryId, String modality, Long minPrice, Long maxPrice, String skill,
                         Double minReputation, String sort, Integer page, Integer size) {
}
