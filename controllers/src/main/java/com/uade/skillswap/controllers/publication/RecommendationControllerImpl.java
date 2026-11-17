package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.RecommendPublicationsUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.common.ControllerSupport.InvalidInputException;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Modality;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.uade.skillswap.controllers.common.ControllerSupport.parseEnum;

public final class RecommendationControllerImpl implements RecommendationController {

    private final RecommendPublicationsUseCase recommend;
    // Registro por nombre: sumar una estrategia es registrarla en CompositionRoot, sin tocar este controller.
    private final Map<String, RecommendationStrategy> strategies;
    private final IdentityService identity;
    private final RecommendationStrategy defaultStrategy;

    public RecommendationControllerImpl(RecommendPublicationsUseCase recommend,
                                        Map<String, RecommendationStrategy> strategies,
                                        RecommendationStrategy defaultStrategy, IdentityService identity) {
        this.recommend = Objects.requireNonNull(recommend, "The recommendation use case is required");
        Map<String, RecommendationStrategy> byName = new HashMap<>();
        Objects.requireNonNull(strategies, "The recommendation strategies are required").forEach((name, strategy) ->
                byName.put(name.strip().toUpperCase(Locale.ROOT), Objects.requireNonNull(strategy)));
        this.strategies = Map.copyOf(byName);
        this.defaultStrategy = Objects.requireNonNull(defaultStrategy, "The default strategy is required");
        this.identity = Objects.requireNonNull(identity, "The identity service is required");
    }

    @Override
    public ControllerResult<Page<PublicationSummary>> recommend(String activeUserId, RecommendationForm form) {
        return ControllerResult.of(() -> {
            UserId userId = currentUser(activeUserId);
            if (form == null || form.filters() == null) {
                throw new InvalidInputException("The recommendation form is required");
            }

            SearchForm search = form.filters();
            SearchFilters filters = new SearchFilters(search.text(), search.categoryId(),
                    parseEnum(Modality.class, search.modality(), "modality"), search.minPrice(), search.maxPrice(),
                    search.tag(), search.minReputation(), parseEnum(SearchSort.class, search.sort(), "sort"),
                    new PageRequest(search.page() == null ? 0 : search.page(),
                            search.size() == null ? PageRequest.DEFAULT_SIZE : search.size()));
            return recommend.recommend(filters, userId, strategyFor(form.strategy()));
        });
    }

    private RecommendationStrategy strategyFor(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return defaultStrategy;
        }
        RecommendationStrategy selected = strategies.get(strategy.strip().toUpperCase(Locale.ROOT));
        if (selected == null) {
            throw new InvalidInputException("Unknown recommendation strategy: " + strategy);
        }
        return selected;
    }

    private UserId currentUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("No active user");
        }
        UserId user = new UserId(userId);
        if (!user.equals(identity.currentUserId())) {
            throw new AccessDeniedException(user, "request recommendations");
        }
        return user;
    }
}
