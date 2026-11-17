package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.application.ports.in.publication.RecommendPublicationsUseCase;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;
import com.uade.skillswap.domain.common.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecommendPublicationsService implements RecommendPublicationsUseCase {

    public static final int DEFAULT_MAX_CANDIDATES = 200;

    private final SearchPublicationsUseCase search;
    private final int maxCandidates;

    public RecommendPublicationsService(SearchPublicationsUseCase search) {
        this(search, DEFAULT_MAX_CANDIDATES);
    }

    public RecommendPublicationsService(SearchPublicationsUseCase search, int maxCandidates) {
        this.search = Objects.requireNonNull(search, "The search use case is required");
        if (maxCandidates < 1) {
            throw new IllegalArgumentException("The maximum number of candidates must be positive");
        }
        this.maxCandidates = maxCandidates;
    }

    @Override
    public Page<PublicationSummary> recommend(SearchFilters filters, UserId activeUserId,
                                               RecommendationStrategy strategy) {
        Objects.requireNonNull(filters, "The search filters are required");
        Objects.requireNonNull(activeUserId, "The active user is required");
        Objects.requireNonNull(strategy, "The recommendation strategy is required");

        // Solo se ordenan los candidatos más recientes, hasta el tope configurado: la memoria queda acotada.
        SearchFilters newestFirst = filters.withSort(SearchSort.MOST_RECENT);
        int pageSize = Math.min(PageRequest.MAX_SIZE, maxCandidates);
        List<PublicationSummary> candidates = new ArrayList<>();
        for (int pageNumber = 0; candidates.size() < maxCandidates; pageNumber++) {
            Page<PublicationSummary> page = search.search(newestFirst.withPage(new PageRequest(pageNumber, pageSize)));
            candidates.addAll(page.items());
            if (!page.hasNext()) {
                break;
            }
        }
        if (candidates.size() > maxCandidates) {
            candidates = new ArrayList<>(candidates.subList(0, maxCandidates));
        }

        List<PublicationSummary> ordered = strategy.order(candidates.stream()
                .filter(publication -> !activeUserId.value().equals(publication.authorId()))
                .toList(), activeUserId);
        long offset = filters.page().offset();
        int start = (int) Math.min(offset, ordered.size());
        int end = (int) Math.min(offset + filters.page().size(), ordered.size());
        return new Page<>(ordered.subList(start, end), filters.page(), ordered.size());
    }
}
