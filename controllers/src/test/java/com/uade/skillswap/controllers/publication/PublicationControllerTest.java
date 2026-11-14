package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.publication.ChangePublicationStatusCommand;
import com.uade.skillswap.application.ports.in.publication.ChangePublicationStatusUseCase;
import com.uade.skillswap.application.ports.in.publication.CreateDraftCommand;
import com.uade.skillswap.application.ports.in.publication.CreateDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.EditDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.GetPublicationUseCase;
import com.uade.skillswap.application.ports.in.publication.ListMyPublicationsUseCase;
import com.uade.skillswap.application.ports.in.publication.PublicationAction;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.in.publication.SearchFilters;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;
import com.uade.skillswap.application.ports.in.publication.SearchSort;
import com.uade.skillswap.controllers.common.ErrorCode;
import com.uade.skillswap.controllers.common.Result;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.PermissionDeniedException;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Tests unitarios de los controllers con casos de uso simulados: conversión de la entrada,
// delegación y traducción uniforme de errores.
class PublicationControllerTest {

    private static final Instant MONDAY = Instant.parse("2026-11-16T15:00:00Z");

    private final CreateDraftUseCase createDraft = mock(CreateDraftUseCase.class);
    private final EditDraftUseCase editDraft = mock(EditDraftUseCase.class);
    private final ChangePublicationStatusUseCase changeStatus = mock(ChangePublicationStatusUseCase.class);
    private final GetPublicationUseCase getPublication = mock(GetPublicationUseCase.class);
    private final ListMyPublicationsUseCase listMine = mock(ListMyPublicationsUseCase.class);
    private final SearchPublicationsUseCase search = mock(SearchPublicationsUseCase.class);

    private final PublicationController controller =
            new PublicationControllerImpl(createDraft, editDraft, changeStatus, getPublication, listMine);
    private final SearchController searchController = new SearchControllerImpl(search);

    private static PublicationForm form(String type, String modality) {
        return new PublicationForm(type, "Mock interview", "Practice", 1L, modality, 60, 2, 100L, Set.of("java"),
                List.of(new PublicationForm.SlotForm(MONDAY, MONDAY.plus(Duration.ofHours(1)))));
    }

    private static PublicationView view() {
        return new PublicationView(1L, "ana", PublicationType.MENTORING, PublicationStatus.DRAFT, "Mock interview",
                "Practice", 1L, Modality.REMOTE, 60L, 2, 2, 100L, Set.of("java"), List.of());
    }

    @Test
    void createDraftConvertsTheFormIntoACommand() {
        when(createDraft.createDraft(any())).thenReturn(view());

        Result<PublicationView> result = controller.createDraft("ana", form("mentoring", "remote"));

        assertThat(result.isOk()).isTrue();
        assertThat(result.value()).isEqualTo(view());
        ArgumentCaptor<CreateDraftCommand> command = ArgumentCaptor.forClass(CreateDraftCommand.class);
        verify(createDraft).createDraft(command.capture());
        assertThat(command.getValue().authorId()).isEqualTo(new UserId("ana"));
        assertThat(command.getValue().type()).isEqualTo(PublicationType.MENTORING);
        assertThat(command.getValue().details().modality()).isEqualTo(Modality.REMOTE);
        assertThat(command.getValue().details().estimatedDuration()).isEqualTo(Duration.ofHours(1));
        assertThat(command.getValue().details().basePrice().amount()).isEqualTo(100);
        assertThat(command.getValue().slots()).hasSize(1);
    }

    @Test
    void invalidInputNeverReachesTheUseCase() {
        assertThat(controller.createDraft(" ", form("MENTORING", "REMOTE")).error().code())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(controller.createDraft("ana", form("WORKSHOP", "REMOTE")).error().code())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(controller.createDraft("ana", form(null, "REMOTE")).error().code())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(controller.createDraft("ana", form("MENTORING", "MOON")).error().code())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(controller.changeStatus("ana", 1, "DELETE", null).error().code())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(createDraft, changeStatus);
    }

    @Test
    void missingFormsOrSlotsAreInvalidInputAndABugIsUnexpected() {
        assertThat(controller.createDraft("ana", null).error().code()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(searchController.search(null).error().code()).isEqualTo(ErrorCode.INVALID_INPUT);
        PublicationForm brokenSlot = new PublicationForm("MENTORING", "t", null, 1L, "REMOTE", null, 1, 10L, null,
                List.of(new PublicationForm.SlotForm(MONDAY, null)));
        assertThat(controller.createDraft("ana", brokenSlot).error().code()).isEqualTo(ErrorCode.INVALID_INPUT);

        when(getPublication.get(any(), org.mockito.ArgumentMatchers.anyLong()))
                .thenThrow(new NullPointerException("Cannot invoke \"String.length()\" because \"title\" is null"));
        Result<PublicationView> bug = controller.getDetail("ana", 1);
        assertThat(bug.error().code()).isEqualTo(ErrorCode.UNEXPECTED);
        assertThat(bug.error().message()).doesNotContain("Cannot invoke");
    }

    @Test
    void aNegativePriceIsInvalidInput() {
        PublicationForm negative = new PublicationForm("MENTORING", "t", null, 1L, "REMOTE", null, 1, -5L, null, null);

        Result<PublicationView> result = controller.createDraft("ana", negative);

        assertThat(result.error().code()).isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(createDraft);
    }

    @Test
    void changeStatusPassesTheActionAndTheReason() {
        when(changeStatus.changeStatus(any())).thenReturn(view());

        controller.changeStatus("ana", 7, "cancel", "No more time");

        verify(changeStatus).changeStatus(new ChangePublicationStatusCommand(new UserId("ana"), 7,
                PublicationAction.CANCEL, "No more time"));
    }

    @Test
    void everyApplicationErrorIsTranslatedTheSameWay() {
        when(changeStatus.changeStatus(any()))
                .thenThrow(new InvalidTransitionException(PublicationStatus.CLOSED, "publish"))
                .thenThrow(new PermissionDeniedException("Only the author can publish a publication"))
                .thenThrow(new BusinessRuleException("The publication cannot be published, missing: title"))
                .thenThrow(new ResourceNotFoundException("Publication", 7))
                .thenThrow(new IllegalStateException("database is down"));

        assertThat(controller.changeStatus("ana", 7, "PUBLISH", null).error().code())
                .isEqualTo(ErrorCode.INVALID_TRANSITION);
        assertThat(controller.changeStatus("ana", 7, "PUBLISH", null).error().code())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(controller.changeStatus("ana", 7, "PUBLISH", null).error().code())
                .isEqualTo(ErrorCode.BUSINESS_RULE);
        assertThat(controller.changeStatus("ana", 7, "PUBLISH", null).error().code())
                .isEqualTo(ErrorCode.NOT_FOUND);
        Result<PublicationView> unexpected = controller.changeStatus("ana", 7, "PUBLISH", null);
        assertThat(unexpected.error().code()).isEqualTo(ErrorCode.UNEXPECTED);
        assertThat(unexpected.error().message()).doesNotContain("database");
    }

    @Test
    void listMineParsesTheStatusAndThePage() {
        when(listMine.listMine(any(), any(), any())).thenReturn(Page.empty(new PageRequest(0, 5)));

        Result<Page<PublicationView>> result = controller.listMine("ana", "published", 0, 5);

        assertThat(result.isOk()).isTrue();
        verify(listMine).listMine(new UserId("ana"), PublicationStatus.PUBLISHED, new PageRequest(0, 5));
        assertThat(controller.listMine("ana", null, -1, 5).error().code()).isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void getDetailDelegatesWithTheActiveUser() {
        when(getPublication.get(new UserId("bruno"), 3)).thenReturn(view());

        assertThat(controller.getDetail("bruno", 3).value()).isEqualTo(view());
    }

    @Test
    void searchBuildsTheFiltersWithDefaults() {
        when(search.search(any())).thenReturn(Page.empty(PageRequest.first()));

        Result<?> result = searchController.search(new SearchForm("java", 2L, "remote", 10L, 100L, "java", 4.0,
                "lowest_price", null, null));

        assertThat(result.isOk()).isTrue();
        verify(search).search(eq(new SearchFilters("java", 2L, Modality.REMOTE, 10L, 100L, "java", 4.0,
                SearchSort.LOWEST_PRICE, PageRequest.first())));
    }

    @Test
    void searchRejectsInvalidFiltersAsInvalidInput() {
        assertThat(searchController.search(new SearchForm(null, null, null, 100L, 10L, null, null, null, null, null))
                .error().code()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(searchController.search(new SearchForm(null, null, null, null, null, null, null, "cheapest", null,
                null)).error().code()).isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(search);
    }
}
