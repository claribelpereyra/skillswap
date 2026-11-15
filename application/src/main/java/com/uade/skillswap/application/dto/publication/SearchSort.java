package com.uade.skillswap.application.dto.publication;

// Orden de los resultados de búsqueda.
public enum SearchSort {

    // Relevancia respecto del texto (reglas en docs/historias/publicaciones.md); sin texto ordena como BEST_REPUTATION.
    RELEVANCE,

    // Mejor reputación del autor primero; a igual reputación, la más reciente.
    BEST_REPUTATION,

    // Menor precio base primero; a igual precio, la más reciente.
    LOWEST_PRICE,

    // Publicada más recientemente primero (cuenta la última vez que pasó a PUBLISHED).
    MOST_RECENT
}
