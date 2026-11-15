package com.uade.skillswap.application.ports.in.publication;

import java.util.List;

// Estrategia intercambiable para ordenar una lista recomendada sin tocar el caso de uso que la usa.
@FunctionalInterface
public interface RecommendationStrategy {

    List<PublicationSummary> order(List<PublicationSummary> publications, SearchFilters filters);
}
