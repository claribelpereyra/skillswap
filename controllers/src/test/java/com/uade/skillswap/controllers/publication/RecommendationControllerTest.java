package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.RecommendPublicationsUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.application.ports.out.publication.RecommendationMetadataQuery;
import com.uade.skillswap.application.usecases.publication.RecentPopularityRecommendationStrategy;
import com.uade.skillswap.application.usecases.publication.SkillAffinityRecommendationStrategy;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Modality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecommendationControllerTest {

    private static final UserId USER = new UserId("ana");
    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");

    private final RecommendPublicationsUseCase recommend = mock(RecommendPublicationsUseCase.class);
    private final IdentityService identity = mock(IdentityService.class);
    private final RecommendationMetadataQuery metadata = mock(RecommendationMetadataQuery.class);
    private final SkillAffinityRecommendationStrategy skillAffinity =
            new SkillAffinityRecommendationStrategy(metadata);
    private final RecentPopularityRecommendationStrategy recentPopularity =
            new RecentPopularityRecommendationStrategy(metadata, Clock.fixed(NOW, ZoneOffset.UTC),
                    Duration.ofDays(30));
    private final RecommendationController controller =
            new RecommendationControllerImpl(recommend, strategies(), skillAffinity, identity);

    private Map<String, RecommendationStrategy> strategies() {
        return Map.of("SKILL_AFFINITY", skillAffinity, "RECENT_POPULARITY", recentPopularity);
    }

    @BeforeEach
    void activeIdentity() {
        when(identity.currentUserId()).thenReturn(USER);
    }

    @Test
    void defaultsToSkillAffinityAndPassesTheActualStrategy() {
        Page<PublicationSummary> page = Page.empty(PageRequest.first());
        when(recommend.recommend(any(), any(), any())).thenReturn(page);

        ControllerResult<Page<PublicationSummary>> result = controller.recommend("ana",
                new RecommendationForm(null,
                        new SearchForm("java", 2L, "remote", 10L, 100L, "java", 4.0, "MOST_RECENT", 1, 5)));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(page);
        ArgumentCaptor<SearchFilters> filters = ArgumentCaptor.forClass(SearchFilters.class);
        ArgumentCaptor<RecommendationStrategy> strategy = ArgumentCaptor.forClass(RecommendationStrategy.class);
        verify(recommend).recommend(filters.capture(), eq(USER), strategy.capture());
        assertThat(filters.getValue()).isEqualTo(new SearchFilters("java", 2L, Modality.REMOTE, 10L, 100L,
                "java", 4.0, SearchSort.MOST_RECENT, new PageRequest(1, 5)));
        assertThat(strategy.getValue()).isSameAs(skillAffinity);
    }

    @Test
    void recentPopularitySelectsItsStrategyWithoutChangingSearchSort() {
        when(recommend.recommend(any(), any(), any())).thenReturn(Page.empty(PageRequest.first()));

        controller.recommend("ana", new RecommendationForm("recent_popularity",
                new SearchForm(null, null, null, null, null, null, null, "BEST_REPUTATION", null, null)));

        ArgumentCaptor<SearchFilters> filters = ArgumentCaptor.forClass(SearchFilters.class);
        ArgumentCaptor<RecommendationStrategy> strategy = ArgumentCaptor.forClass(RecommendationStrategy.class);
        verify(recommend).recommend(filters.capture(), eq(USER), strategy.capture());
        assertThat(filters.getValue().sort()).isEqualTo(SearchSort.BEST_REPUTATION);
        assertThat(strategy.getValue()).isSameAs(recentPopularity);
    }

    @Test
    void configuredDefaultIsUsedWhenTheScreenDoesNotChooseAStrategy() {
        when(recommend.recommend(any(), any(), any())).thenReturn(Page.empty(PageRequest.first()));
        var configured = new RecommendationControllerImpl(recommend, strategies(), recentPopularity, identity);
        var result = configured.recommend("ana", new RecommendationForm(null,
                new SearchForm(null, null, null, null, null, null, null, null, null, null)));
        assertThat(result.isSuccess()).isTrue();
        verify(recommend).recommend(any(), eq(USER), eq(recentPopularity));
    }

    @Test
    void aNewRegisteredStrategyIsSelectableWithoutChangingTheController() {
        RecommendationStrategy custom = (publications, user) -> publications;
        when(recommend.recommend(any(), any(), any())).thenReturn(Page.empty(PageRequest.first()));
        var withThird = new RecommendationControllerImpl(recommend,
                Map.of("SKILL_AFFINITY", skillAffinity, "RECENT_POPULARITY", recentPopularity, "custom", custom),
                skillAffinity, identity);

        var result = withThird.recommend("ana", new RecommendationForm(" Custom ",
                new SearchForm(null, null, null, null, null, null, null, null, null, null)));

        assertThat(result.isSuccess()).isTrue();
        verify(recommend).recommend(any(), eq(USER), eq(custom));
    }

    @Test
    void activeIdentityAndFormsAreValidatedBeforeCallingTheUseCase() {
        assertThat(controller.recommend(" ", new RecommendationForm(null,
                new SearchForm(null, null, null, null, null, null, null, null, null, null))).getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(controller.recommend("bruno", new RecommendationForm(null,
                new SearchForm(null, null, null, null, null, null, null, null, null, null))).getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(controller.recommend("ana", null).getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(controller.recommend("ana", new RecommendationForm(null, null)).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(controller.recommend("ana", new RecommendationForm("unknown",
                new SearchForm(null, null, null, null, null, null, null, null, null, null))).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT);
        verifyNoInteractions(recommend);
    }
}
