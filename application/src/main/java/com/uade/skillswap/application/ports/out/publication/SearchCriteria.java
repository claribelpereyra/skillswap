package com.uade.skillswap.application.ports.out.publication;

import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.domain.publication.Modality;

import java.util.Set;

// Criterios ya normalizados que recibe la consulta de búsqueda. Siempre se buscan solo publicaciones
// PUBLICADAS. Un campo null es "sin filtro".
// - text y skill llegan sin espacios de más y en minúsculas.
// - categoryIds trae la categoría pedida y todo lo que cuelga de ella.
// - sort nunca es RELEVANCE sin texto (el caso de uso lo cambia por BEST_REPUTATION).
public record SearchCriteria(String text, Set<Long> categoryIds, Modality modality, Long minPrice, Long maxPrice,
                             String skill, Double minReputation, SearchSort sort) {
}
