package com.uade.skillswap.application.ports.out.publication;

import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.domain.publication.Modality;

import java.util.Set;

// Filtros ya normalizados por el caso de uso; la consulta busca solo publicaciones PUBLISHED.
public record SearchCriteria(String text, Set<Long> categoryIds, Modality modality, Long minPrice, Long maxPrice,
                             String skill, Double minReputation, SearchSort sort) {
}
