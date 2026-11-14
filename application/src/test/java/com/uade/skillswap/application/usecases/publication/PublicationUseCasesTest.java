package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.publication.ChangePublicationStatusCommand;
import com.uade.skillswap.application.ports.in.publication.CreateDraftCommand;
import com.uade.skillswap.application.ports.in.publication.EditDraftCommand;
import com.uade.skillswap.application.ports.in.publication.PublicationAction;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.in.publication.SlotInput;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.AuthorProfileQuery;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.PermissionDeniedException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.IncompletePublicationException;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicationUseCasesTest {

    private static final UserId AUTHOR = new UserId("ana");
    private static final UserId OTHER = new UserId("bruno");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-16T15:00:00Z");
    private static final SlotInput MONDAY_SLOT = new SlotInput(MONDAY, MONDAY.plus(Duration.ofHours(1)));
    private static final SlotInput TUESDAY_SLOT = new SlotInput(MONDAY.plus(Duration.ofDays(1)),
            MONDAY.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)));

    private final InMemoryPublicationRepository repository = new InMemoryPublicationRepository();
    private final AuthorProfileQuery profiles = mock(AuthorProfileQuery.class);
    private final UnitOfWork unitOfWork = Supplier::get;
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CreateDraftService createDraft = new CreateDraftService(repository, profiles, unitOfWork, clock);
    private final EditDraftService editDraft = new EditDraftService(repository, unitOfWork);
    private final ChangePublicationStatusService changeStatus =
            new ChangePublicationStatusService(repository, unitOfWork, clock);

    @BeforeEach
    void profilesOfTheAuthors() {
        when(profiles.accountTypeOf(any())).thenReturn(Optional.empty());
        when(profiles.accountTypeOf(AUTHOR)).thenReturn(Optional.of(AccountType.PROFESSIONAL));
        when(profiles.accountTypeOf(OTHER)).thenReturn(Optional.of(AccountType.PROFESSIONAL));
    }

    private static PublicationDetails mentoring(int capacity) {
        return new PublicationDetails("Mock interview", "Practice a technical interview", 1L, Modality.REMOTE,
                Duration.ofMinutes(60), capacity, new SkillCoins(100), Set.of("java"));
    }

    private PublicationView draftWithSlots(SlotInput... slots) {
        return createDraft.createDraft(new CreateDraftCommand(AUTHOR, PublicationType.MENTORING, mentoring(1),
                List.of(slots)));
    }

    private PublicationView change(UserId actor, long id, PublicationAction action, String reason) {
        return changeStatus.changeStatus(new ChangePublicationStatusCommand(actor, id, action, reason));
    }

    // --- Crear borrador ---

    @Test
    void aDraftIsCreatedWithItsSlotsAndSaved() {
        PublicationView view = draftWithSlots(MONDAY_SLOT, TUESDAY_SLOT);

        assertThat(view.id()).isNotNull();
        assertThat(view.status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(view.authorId()).isEqualTo("ana");
        assertThat(view.basePrice()).isEqualTo(100L);
        assertThat(view.durationMinutes()).isEqualTo(60L);
        assertThat(view.availableSpots()).isEqualTo(1);
        assertThat(view.slots()).extracting(PublicationView.SlotView::startsAt)
                .containsExactly(MONDAY_SLOT.startsAt(), TUESDAY_SLOT.startsAt());
        assertThat(repository.findById(view.id())).isPresent();
    }

    @Test
    void aUserWithoutProfileCannotCreateADraft() {
        CreateDraftCommand command = new CreateDraftCommand(new UserId("ghost"), PublicationType.MENTORING,
                mentoring(1), List.of());

        assertThatThrownBy(() -> createDraft.createDraft(command)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(repository.saves).isZero();
    }

    @Test
    void aProfessionalCannotCreateAChallenge() {
        CreateDraftCommand command = new CreateDraftCommand(AUTHOR, PublicationType.CHALLENGE, mentoring(1),
                List.of());

        assertThatThrownBy(() -> createDraft.createDraft(command)).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A PROFESSIONAL account cannot publish a CHALLENGE");
        assertThat(repository.saves).isZero();
    }

    @Test
    void overlappingSlotsAreRejectedAndNothingIsSaved() {
        SlotInput overlapping = new SlotInput(MONDAY.plus(Duration.ofMinutes(30)), MONDAY.plus(Duration.ofHours(2)));

        assertThatThrownBy(() -> draftWithSlots(MONDAY_SLOT, overlapping))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot overlaps another slot of the publication");
        assertThat(repository.saves).isZero();
    }

    // --- Editar borrador ---

    @Test
    void theAuthorEditsTheDetailsAndReplacesTheSlots() {
        long id = draftWithSlots(MONDAY_SLOT).id();
        SlotInput mondayLater = new SlotInput(MONDAY.plus(Duration.ofMinutes(30)),
                MONDAY.plus(Duration.ofMinutes(90)));

        PublicationView edited = editDraft.editDraft(new EditDraftCommand(AUTHOR, id, mentoring(3),
                List.of(mondayLater, TUESDAY_SLOT)));

        assertThat(edited.capacity()).isEqualTo(3);
        assertThat(edited.slots()).extracting(PublicationView.SlotView::startsAt)
                .containsExactlyInAnyOrder(mondayLater.startsAt(), TUESDAY_SLOT.startsAt());
    }

    @Test
    void anotherUserCannotEditTheDraft() {
        long id = draftWithSlots(MONDAY_SLOT).id();

        assertThatThrownBy(() -> editDraft.editDraft(new EditDraftCommand(OTHER, id, mentoring(2), List.of())))
                .isExactlyInstanceOf(PermissionDeniedException.class)
                .hasMessage("Only the author can edit a publication");
        assertThat(repository.findById(id).orElseThrow().getDetails().capacity()).isEqualTo(1);
    }

    // Si la edición falla a mitad de camino, no queda nada a medias: ni el cupo nuevo ni franjas sueltas.
    @Test
    void anEditWithOverlappingSlotsChangesNothing() {
        long id = draftWithSlots(MONDAY_SLOT).id();
        int savesBefore = repository.saves;
        SlotInput overlapping = new SlotInput(TUESDAY_SLOT.startsAt().plus(Duration.ofMinutes(30)),
                TUESDAY_SLOT.endsAt().plus(Duration.ofMinutes(30)));

        assertThatThrownBy(() -> editDraft.editDraft(new EditDraftCommand(AUTHOR, id, mentoring(3),
                List.of(MONDAY_SLOT, TUESDAY_SLOT, overlapping))))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot overlaps another slot of the publication");

        assertThat(repository.saves).isEqualTo(savesBefore);
        assertThat(repository.findById(id).orElseThrow().getDetails().capacity()).isEqualTo(1);
        assertThat(repository.findById(id).orElseThrow().getSlots()).hasSize(1);
    }

    @Test
    void aPublishedPublicationCannotBeEdited() {
        long id = draftWithSlots(MONDAY_SLOT).id();
        change(AUTHOR, id, PublicationAction.PUBLISH, null);

        assertThatThrownBy(() -> editDraft.editDraft(new EditDraftCommand(AUTHOR, id, mentoring(2),
                List.of(MONDAY_SLOT)))).isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void editingAnUnknownPublicationFails() {
        assertThatThrownBy(() -> editDraft.editDraft(new EditDraftCommand(AUTHOR, 99, mentoring(1), List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Cambiar estado ---

    @Test
    void theAuthorTakesThePublicationThroughItsWholeLifecycle() {
        long id = draftWithSlots(MONDAY_SLOT).id();

        assertThat(change(AUTHOR, id, PublicationAction.PUBLISH, null).status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(change(AUTHOR, id, PublicationAction.PAUSE, null).status()).isEqualTo(PublicationStatus.PAUSED);
        assertThat(change(AUTHOR, id, PublicationAction.RESUME, null).status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(change(AUTHOR, id, PublicationAction.CLOSE, null).status()).isEqualTo(PublicationStatus.CLOSED);
        assertThat(repository.findById(id).orElseThrow().getHistory()).hasSize(5)
                .allSatisfy(t -> assertThat(t.occurredAt()).isEqualTo(NOW));
    }

    @Test
    void theAuthorCancelsWithAReason() {
        long id = draftWithSlots(MONDAY_SLOT).id();
        change(AUTHOR, id, PublicationAction.PUBLISH, null);

        PublicationView cancelled = change(AUTHOR, id, PublicationAction.CANCEL, "No more availability");

        assertThat(cancelled.status()).isEqualTo(PublicationStatus.CANCELLED);
        assertThat(repository.findById(id).orElseThrow().getHistory().get(2).reason())
                .isEqualTo("No more availability");
    }

    // Caso de aceptación 2: rechazar la publicación de una oferta incompleta.
    @Test
    void anIncompleteOfferIsNotPublished() {
        PublicationView draft = createDraft.createDraft(new CreateDraftCommand(AUTHOR, PublicationType.SERVICE,
                new PublicationDetails("Portfolio review", null, null, Modality.REMOTE, null, 1, null, null),
                List.of()));
        int savesBefore = repository.saves;

        assertThatThrownBy(() -> change(AUTHOR, draft.id(), PublicationAction.PUBLISH, null))
                .isInstanceOfSatisfying(IncompletePublicationException.class, e -> assertThat(e.getMissing())
                        .containsExactly("category", "base price", "at least one time slot"));
        assertThat(repository.saves).isEqualTo(savesBefore);
        assertThat(repository.findById(draft.id()).orElseThrow().getStatus()).isEqualTo(PublicationStatus.DRAFT);
    }

    @Test
    void anInvalidTransitionIsRejectedAndNotSaved() {
        long id = draftWithSlots(MONDAY_SLOT).id();
        int savesBefore = repository.saves;

        assertThatThrownBy(() -> change(AUTHOR, id, PublicationAction.RESUME, null))
                .isInstanceOf(InvalidTransitionException.class);
        assertThat(repository.saves).isEqualTo(savesBefore);
    }

    @Test
    void anotherUserCannotChangeTheStatus() {
        long id = draftWithSlots(MONDAY_SLOT).id();

        assertThatThrownBy(() -> change(OTHER, id, PublicationAction.PUBLISH, null))
                .isExactlyInstanceOf(PermissionDeniedException.class)
                .hasMessage("Only the author can publish a publication");
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(PublicationStatus.DRAFT);
    }

    @Test
    void changingTheStatusOfAnUnknownPublicationFails() {
        assertThatThrownBy(() -> change(AUTHOR, 99, PublicationAction.PUBLISH, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
