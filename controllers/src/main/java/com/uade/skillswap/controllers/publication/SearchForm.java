package com.uade.skillswap.controllers.publication;

public record SearchForm(String text, Long categoryId, String modality, Long minPrice, Long maxPrice, String tag,
                         Double minReputation, String sort, Integer page, Integer size) {
}
