package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.publication.PublicationSummary;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.in.publication.SearchFilters;
import com.uade.skillswap.application.ports.in.publication.SearchSort;
import com.uade.skillswap.application.ports.out.publication.CategoryTree;
import com.uade.skillswap.application.ports.out.publication.PublicationSearchQuery;
import com.uade.skillswap.application.ports.out.publication.SearchCriteria;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicationQueriesTest {

    private static final UserId AUTHOR = new UserId("ana");
    private static final UserId VISITOR = new UserId("bruno");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final InMemoryPublicationRepository repository = new InMemoryPublicationRepository();
    private final GetPublicationService getPublication = new GetPublicationService(repository);
    private final ListMyPublicationsService listMine = new ListMyPublicationsService(repository);

    private final PublicationSearchQuery searchQuery = mock(PublicationSearchQuery.class);
    private final CategoryTree categories = mock(CategoryTree.class);
    private final SearchPublicationsService search = new SearchPublicationsService(searchQuery, categories);

    private long saveDraft(UserId author, String title) {
        PublicationDetails details = new PublicationDetails(title, null, 1L, Modality.REMOTE, null, 1,
                new SkillCoins(50), Set.of());
        Publication draft = Publication.draft(author, AccountType.PROFESSIONAL, PublicationType.MENTORING,
                details, NOW);
        draft.addSlot(author, NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)));
        return repository.save(draft).getId();
    }

    private long savePublished(UserId author, String title) {
        long id = saveDraft(author, title);
        Publication publication = repository.findById(id).orElseThrow();
        publication.publish(author, NOW);
        repository.save(publication);
        return id;
    }

    // --- Ver detalle ---

    @Test
    void anyoneSeesThePublishedDetail() {
        long id = savePublished(AUTHOR, "Mock interview");

        PublicationView view = getPublication.get(VISITOR, id);

        assertThat(view.title()).isEqualTo("Mock interview");
        assertThat(view.status()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void aDraftIsOnlySeenByItsAuthorAndForOthersItDoesNotExist() {
        long id = saveDraft(AUTHOR, "Secret draft");

        assertThat(getPublication.get(AUTHOR, id).status()).isEqualTo(PublicationStatus.DRAFT);
        assertThatThrownBy(() -> getPublication.get(VISITOR, id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anUnknownPublicationIsNotFound() {
        assertThatThrownBy(() -> getPublication.get(VISITOR, 42)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Listar mis publicaciones ---

    @Test
    void theAuthorListsAllTheirPublicationsPaginatedNewestFirst() {
        long first = saveDraft(AUTHOR, "First");
        long second = savePublished(AUTHOR, "Second");
        long third = saveDraft(AUTHOR, "Third");
        savePublished(VISITOR, "Not mine");

        Page<PublicationView> firstPage = listMine.listMine(AUTHOR, null, new PageRequest(0, 2));
        Page<PublicationView> secondPage = listMine.listMine(AUTHOR, null, new PageRequest(1, 2));

        assertThat(firstPage.items()).extracting(PublicationView::id).containsExactly(third, second);
        assertThat(firstPage.totalItems()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.items()).extracting(PublicationView::id).containsExactly(first);
    }

    @Test
    void theAuthorFiltersTheirPublicationsByStatus() {
        saveDraft(AUTHOR, "Draft");
        long published = savePublished(AUTHOR, "Published");

        Page<PublicationView> page = listMine.listMine(AUTHOR, PublicationStatus.PUBLISHED, null);

        assertThat(page.items()).extracting(PublicationView::id).containsExactly(published);
        assertThat(page.request()).isEqualTo(PageRequest.first());
    }

    // --- Buscar ---

    private SearchCriteria searchAndCapture(SearchFilters filters) {
        Page<PublicationSummary> expected = new Page<>(List.of(), filters.page(), filters.page().offset());
        when(searchQuery.search(any(), any())).thenReturn(expected);

        assertThat(search.search(filters)).isSameAs(expected);

        ArgumentCaptor<SearchCriteria> criteria = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(searchQuery).search(criteria.capture(), eq(filters.page()));
        return criteria.getValue();
    }

    @Test
    void theFiltersAreNormalizedAndTheCategoryIncludesItsSubtree() {
        when(categories.idsUnder(2L)).thenReturn(Optional.of(Set.of(2L, 3L, 10L)));
        SearchFilters filters = new SearchFilters("  Java Interview ", 2L, Modality.REMOTE, 10L, 200L, " JAVA ",
                4.0, SearchSort.LOWEST_PRICE, new PageRequest(1, 10));

        SearchCriteria criteria = searchAndCapture(filters);

        assertThat(criteria).isEqualTo(new SearchCriteria("java interview", Set.of(2L, 3L, 10L), Modality.REMOTE,
                10L, 200L, "java", 4.0, SearchSort.LOWEST_PRICE));
    }

    @Test
    void withOnlyATextItSearchesByRelevanceOnTheFirstPage() {
        SearchCriteria criteria = searchAndCapture(new SearchFilters("mentoring", null, null, null, null, null,
                null, null, null));

        assertThat(criteria.sort()).isEqualTo(SearchSort.RELEVANCE);
        assertThat(criteria.categoryIds()).isNull();
        assertThat(criteria.skill()).isNull();
        verifyNoInteractions(categories);
    }

    @Test
    void relevanceWithoutTextFallsBackToBestReputation() {
        SearchCriteria criteria = searchAndCapture(SearchFilters.none());

        assertThat(criteria.text()).isNull();
        assertThat(criteria.sort()).isEqualTo(SearchSort.BEST_REPUTATION);
    }

    @Test
    void anUnknownCategoryIsNotFound() {
        when(categories.idsUnder(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> search.search(new SearchFilters(null, 99L, null, null, null, null, null, null,
                null))).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(searchQuery);
    }

    @Test
    void invalidFiltersAreRejected() {
        assertThatThrownBy(() -> new SearchFilters(null, null, null, 100L, 10L, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchFilters(null, null, null, -1L, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchFilters(null, null, null, null, null, null, 6.0, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theSearchReturnsThePageTheQueryFound() {
        PageRequest page = new PageRequest(0, 1);
        PublicationSummary row = new PublicationSummary(1, "ana", 4.5, PublicationType.MENTORING, "Mock interview",
                1L, Modality.REMOTE, 100, 1, Set.of("java"), NOW);
        when(searchQuery.search(any(), eq(page))).thenReturn(new Page<>(List.of(row), page, 3));

        Page<PublicationSummary> result = search.search(new SearchFilters("mock", null, null, null, null, null,
                null, null, page));

        assertThat(result.items()).containsExactly(row);
        assertThat(result.totalPages()).isEqualTo(3);
    }
}
