package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.in.publication.PublicationSummary;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.SearchFilters;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;
import com.uade.skillswap.application.ports.in.publication.SearchSort;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendPublicationsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final SearchPublicationsUseCase search = mock(SearchPublicationsUseCase.class);
    private final RecommendPublicationsService service = new RecommendPublicationsService(search);

    @Test
    void aDifferentStrategyCanBeInjectedWithoutChangingTheUseCase() {
        PublicationSummary javaMatch = new PublicationSummary(11L, "ana", 4.8, PublicationType.MENTORING,
                "Java Mentor", 2L, Modality.REMOTE, 120L, 2, Set.of("java", "spring"), NOW);
        PublicationSummary frontendMatch = new PublicationSummary(12L, "bruno", 4.2, PublicationType.SERVICE,
                "Frontend review", 3L, Modality.ON_SITE, 90L, 1, Set.of("frontend"), NOW.plusSeconds(3600));

        when(search.search(any())).thenReturn(new Page<>(List.of(frontendMatch, javaMatch), PageRequest.first(), 2));
        SearchFilters filters = new SearchFilters(null, null, null, null, null, "java", null,
                SearchSort.SKILL_MATCH, PageRequest.first());

        Page<PublicationSummary> withSkillStrategy = service.recommend(filters, new SkillAffinityRecommendationStrategy());
        assertThat(withSkillStrategy.items()).containsExactly(javaMatch, frontendMatch);

        Page<PublicationSummary> withRecentStrategy = service.recommend(filters, new RecentPopularityRecommendationStrategy());
        assertThat(withRecentStrategy.items()).containsExactly(frontendMatch, javaMatch);

        RecommendationStrategy customStrategy = (publications, ignored) -> publications.stream()
                .sorted((left, right) -> Long.compare(right.basePrice(), left.basePrice()))
                .toList();
        Page<PublicationSummary> custom = service.recommend(filters, customStrategy);
        assertThat(custom.items()).containsExactly(javaMatch, frontendMatch);
    }
}
