package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;
import com.uade.skillswap.application.ports.out.publication.RecommendationMetadataQuery;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendPublicationsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final SearchPublicationsUseCase search = mock(SearchPublicationsUseCase.class);
    private final RecommendationMetadataQuery metadata = mock(RecommendationMetadataQuery.class);
    private final RecommendPublicationsService service = new RecommendPublicationsService(search);

    @Test
    void itOrdersAllCandidatePagesBeforeApplyingTheRequestedPage() {
        List<PublicationSummary> firstPage = IntStream.rangeClosed(1, 100)
                .mapToObj(this::publication)
                .toList();
        PublicationSummary last = publication(101);
        PageRequest candidatePage = new PageRequest(0, PageRequest.MAX_SIZE);
        when(search.search(any()))
                .thenReturn(new Page<>(firstPage, candidatePage, 101))
                .thenReturn(new Page<>(List.of(last), new PageRequest(1, PageRequest.MAX_SIZE), 101));
        SearchFilters filters = new SearchFilters("java", 4L, Modality.REMOTE, 10L, 200L, "backend", 2.5,
                SearchSort.RELEVANCE, new PageRequest(0, 10), "ana");

        Page<PublicationSummary> result = service.recommend(filters, (publications, ignored) ->
                publications.stream().sorted((left, right) -> Long.compare(right.id(), left.id())).toList());

        assertThat(result.items()).extracting(PublicationSummary::id).containsExactly(101L, 100L, 99L, 98L,
                97L, 96L, 95L, 94L, 93L, 92L);
        assertThat(result.totalItems()).isEqualTo(101);
        verify(search).search(filters.withPage(new PageRequest(0, PageRequest.MAX_SIZE)));
        verify(search).search(filters.withPage(new PageRequest(1, PageRequest.MAX_SIZE)));
    }

    @Test
    void skillAffinityUsesNormalizedProfileSkillsAndInterests() {
        PublicationSummary javaMatch = publication(2, Set.of("JAVA"));
        PublicationSummary cloudMatch = publication(1, Set.of("cloud", "java"));
        when(metadata.skillsAndInterestsFor("ana")).thenReturn(Set.of("java", "Cloud"));
        SearchFilters filters = new SearchFilters(null, null, null, null, null, "unrelated", null,
                SearchSort.RELEVANCE, PageRequest.first(), "ana");

        List<PublicationSummary> result = new SkillAffinityRecommendationStrategy(metadata)
                .order(List.of(cloudMatch, javaMatch), filters);

        assertThat(result).containsExactly(cloudMatch, javaMatch);
        verify(metadata).skillsAndInterestsFor("ana");
    }

    @Test
    void recentPopularityUsesBookingCountsWithinItsConfiguredWindow() {
        PublicationSummary lessPopular = publication(1, Set.of(), NOW.plus(Duration.ofDays(10)));
        PublicationSummary morePopular = publication(2, Set.of(), NOW.minus(Duration.ofDays(10)));
        Set<Long> publicationIds = Set.of(1L, 2L);
        Instant since = NOW.minus(Duration.ofDays(30));
        when(metadata.recentBookingCounts(publicationIds, since)).thenReturn(Map.of(1L, 1L, 2L, 5L));
        RecentPopularityRecommendationStrategy strategy = new RecentPopularityRecommendationStrategy(metadata,
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(30));

        List<PublicationSummary> result = strategy.order(List.of(lessPopular, morePopular), SearchFilters.none());

        assertThat(result).containsExactly(morePopular, lessPopular);
        verify(metadata).recentBookingCounts(publicationIds, since);
    }

    @Test
    void recommendationRequiresAnExplicitStrategy() {
        assertThatThrownBy(() -> service.recommend(SearchFilters.none(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("The recommendation strategy is required");
    }

    private PublicationSummary publication(int id) {
        return publication(id, Set.of(), NOW);
    }

    private PublicationSummary publication(int id, Set<String> tags) {
        return publication(id, tags, NOW);
    }

    private PublicationSummary publication(int id, Set<String> tags, Instant publishedAt) {
        return new PublicationSummary(id, "ana", 4.8, PublicationType.MENTORING, "Publication " + id, 2L,
                Modality.REMOTE, 120L, 2, tags, publishedAt);
    }
}
