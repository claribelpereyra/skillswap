package com.uade.skillswap.domain.publication;

import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.PermissionDeniedException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class PublicationTest {

    private static final UserId AUTHOR = new UserId("author");
    private static final UserId CLIENT = new UserId("client");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant SLOT_START = Instant.parse("2026-11-20T15:00:00Z");

    private static PublicationDetails completeDetails(int capacity) {
        return new PublicationDetails("Mock interview", "Practice a technical interview", 1L, Modality.REMOTE,
                Duration.ofHours(1), capacity, new SkillCoins(100), Set.of("java", "interviews"));
    }

    private static Publication draftMentoring(int capacity) {
        return Publication.draft(AUTHOR, AccountType.PROFESSIONAL, PublicationType.MENTORING,
                completeDetails(capacity), NOW);
    }

    private static TimeSlot addSlot(Publication publication, int hoursFromStart) {
        Instant start = SLOT_START.plus(Duration.ofHours(hoursFromStart));
        return publication.addSlot(AUTHOR, start, start.plus(Duration.ofHours(1)));
    }

    private static Publication published(int capacity, int slots) {
        Publication publication = draftMentoring(capacity);
        for (int i = 0; i < slots; i++) {
            addSlot(publication, i * 2);
        }
        publication.publish(AUTHOR, NOW);
        return publication;
    }

    // --- Creación y tipo ---

    @Test
    void aNewPublicationStartsAsADraftWithTheAuthorInTheHistory() {
        Publication publication = draftMentoring(1);

        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(publication.getHistory()).containsExactly(
                new StateTransition<>(null, PublicationStatus.DRAFT, AUTHOR, NOW, "Draft created by the author"));
    }

    @Test
    void everyTransitionWithoutAUserReasonRecordsADefaultOne() {
        Publication resumedThenClosed = published(1, 1);
        resumedThenClosed.pause(AUTHOR, NOW);
        resumedThenClosed.resume(AUTHOR, NOW);
        resumedThenClosed.close(AUTHOR, NOW);

        Publication pausedThenClosed = published(1, 1);
        pausedThenClosed.pause(AUTHOR, NOW);
        pausedThenClosed.close(AUTHOR, NOW);

        assertThat(resumedThenClosed.getHistory()).extracting(StateTransition::reason).containsExactly(
                "Draft created by the author", "Published by the author", "Paused by the author",
                "Resumed by the author", "Closed by the author");
        assertThat(pausedThenClosed.getHistory()).extracting(StateTransition::reason).containsExactly(
                "Draft created by the author", "Published by the author", "Paused by the author",
                "Closed by the author");
    }

    @Test
    void aProfessionalPublishesMentoringAndServicesAndAnOrganizationPublishesChallenges() {
        assertThat(PublicationType.MENTORING.canBePublishedBy(AccountType.PROFESSIONAL)).isTrue();
        assertThat(PublicationType.SERVICE.canBePublishedBy(AccountType.PROFESSIONAL)).isTrue();
        assertThat(PublicationType.CHALLENGE.canBePublishedBy(AccountType.ORGANIZATION)).isTrue();

        assertThatThrownBy(() -> Publication.draft(AUTHOR, AccountType.PROFESSIONAL, PublicationType.CHALLENGE,
                completeDetails(1), NOW)).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A PROFESSIONAL account cannot publish a CHALLENGE");
        assertThatThrownBy(() -> Publication.draft(AUTHOR, AccountType.ORGANIZATION, PublicationType.MENTORING,
                completeDetails(1), NOW)).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A ORGANIZATION account cannot publish a MENTORING");
    }

    @Test
    void theDetailsRejectAZeroCapacityOrANonPositiveDuration() {
        assertThatThrownBy(() -> new PublicationDetails("t", null, 1L, Modality.REMOTE, null, 0, SkillCoins.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicationDetails("t", null, 1L, Modality.REMOTE, Duration.ZERO, 1,
                SkillCoins.ZERO, null)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- Publicar (casos 1 y 2) ---

    @Test
    void aCompleteDraftIsPublished() {
        Publication publication = draftMentoring(1);
        addSlot(publication, 0);

        publication.publish(AUTHOR, NOW);

        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(publication.acceptsRequests()).isTrue();
    }

    @Test
    void anIncompleteOfferIsNotPublishedAndSaysWhatIsMissing() {
        PublicationDetails incomplete = new PublicationDetails(" ", null, null, null, null, 1, null, null);
        Publication publication = Publication.draft(AUTHOR, AccountType.PROFESSIONAL, PublicationType.SERVICE,
                incomplete, NOW);

        assertThatThrownBy(() -> publication.publish(AUTHOR, NOW))
                .isInstanceOfSatisfying(IncompletePublicationException.class, e -> assertThat(e.getMissing())
                        .containsExactly("title", "category", "base price", "at least one time slot"));
        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(publication.getHistory()).hasSize(1);
    }

    @Test
    void aDraftWithoutSlotsIsNotPublished() {
        Publication publication = draftMentoring(1);

        assertThatThrownBy(() -> publication.publish(AUTHOR, NOW))
                .isInstanceOfSatisfying(IncompletePublicationException.class,
                        e -> assertThat(e.getMissing()).containsExactly("at least one time slot"));
    }

    @Test
    void aFreePublicationCanBePublished() {
        PublicationDetails free = new PublicationDetails("Free intro", null, 1L, Modality.ON_SITE, null, 1,
                SkillCoins.ZERO, null);
        Publication publication = Publication.draft(AUTHOR, AccountType.PROFESSIONAL, PublicationType.MENTORING,
                free, NOW);
        addSlot(publication, 0);

        publication.publish(AUTHOR, NOW);

        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    // --- Transiciones válidas ---

    @Test
    void aPublishedPublicationIsPausedAndResumed() {
        Publication publication = published(1, 1);

        publication.pause(AUTHOR, NOW);
        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.PAUSED);
        assertThat(publication.acceptsRequests()).isFalse();

        publication.resume(AUTHOR, NOW);
        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void aPublishedOrPausedPublicationIsClosed() {
        Publication publishedOne = published(1, 1);
        publishedOne.close(AUTHOR, NOW);
        assertThat(publishedOne.getStatus()).isEqualTo(PublicationStatus.CLOSED);

        Publication pausedOne = published(1, 1);
        pausedOne.pause(AUTHOR, NOW);
        pausedOne.close(AUTHOR, NOW);
        assertThat(pausedOne.getStatus()).isEqualTo(PublicationStatus.CLOSED);
    }

    @Test
    void aDraftPublishedOrPausedPublicationIsCancelledWithAReason() {
        Publication draft = draftMentoring(1);
        draft.cancel(AUTHOR, "No longer offered", NOW);
        assertThat(draft.getStatus()).isEqualTo(PublicationStatus.CANCELLED);

        Publication publishedOne = published(1, 1);
        publishedOne.cancel(AUTHOR, "No longer offered", NOW);
        assertThat(publishedOne.getStatus()).isEqualTo(PublicationStatus.CANCELLED);
        assertThat(publishedOne.getHistory().get(publishedOne.getHistory().size() - 1).reason())
                .isEqualTo("No longer offered");

        Publication pausedOne = published(1, 1);
        pausedOne.pause(AUTHOR, NOW);
        pausedOne.cancel(AUTHOR, "No longer offered", NOW);
        assertThat(pausedOne.getStatus()).isEqualTo(PublicationStatus.CANCELLED);
    }

    @Test
    void cancellingNeedsAReason() {
        Publication publication = published(1, 1);

        assertThatThrownBy(() -> publication.cancel(AUTHOR, " ", NOW)).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A reason is required to cancel a publication");
        assertThatThrownBy(() -> publication.cancel(AUTHOR, null, NOW)).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A reason is required to cancel a publication");
        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void theHistoryKeepsEveryTransitionWithActorDateAndReason() {
        Publication publication = draftMentoring(1);
        addSlot(publication, 0);
        Instant later = NOW.plus(Duration.ofDays(1));

        publication.publish(AUTHOR, NOW);
        publication.pause(AUTHOR, later);
        publication.cancel(AUTHOR, "Moving abroad", later);

        assertThat(publication.getHistory()).extracting(StateTransition::from, StateTransition::to)
                .containsExactly(
                        tuple(null, PublicationStatus.DRAFT),
                        tuple(PublicationStatus.DRAFT, PublicationStatus.PUBLISHED),
                        tuple(PublicationStatus.PUBLISHED, PublicationStatus.PAUSED),
                        tuple(PublicationStatus.PAUSED, PublicationStatus.CANCELLED));
        StateTransition<PublicationStatus> last = publication.getHistory().get(3);
        assertThat(last.actor()).isEqualTo(AUTHOR);
        assertThat(last.occurredAt()).isEqualTo(later);
        assertThat(last.reason()).isEqualTo("Moving abroad");
    }

    // --- Transiciones inválidas ---

    @Test
    void aDraftCannotBePausedResumedOrClosed() {
        Publication publication = draftMentoring(1);

        assertThatThrownBy(() -> publication.pause(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.resume(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.close(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void aPublishedPublicationCannotBePublishedAgainResumedOrEdited() {
        Publication publication = published(1, 1);

        assertThatThrownBy(() -> publication.publish(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.resume(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.edit(AUTHOR, completeDetails(2)))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> addSlot(publication, 10)).isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void aPausedPublicationCannotBePausedOrPublished() {
        Publication publication = published(1, 1);
        publication.pause(AUTHOR, NOW);

        assertThatThrownBy(() -> publication.pause(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.publish(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void aClosedPublicationCannotBeReopenedNorChanged() {
        Publication publication = published(1, 1);
        publication.close(AUTHOR, NOW);

        assertThatThrownBy(() -> publication.publish(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.resume(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.pause(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.close(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.cancel(AUTHOR, "reason", NOW))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.edit(AUTHOR, completeDetails(1)))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void aCancelledPublicationRejectsEveryAction() {
        Publication publication = published(1, 1);
        publication.cancel(AUTHOR, "reason", NOW);

        assertThatThrownBy(() -> publication.publish(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.resume(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.pause(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.close(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> publication.cancel(AUTHOR, "again", NOW))
                .isInstanceOf(InvalidTransitionException.class);
        assertThat(publication.getHistory()).hasSize(3);
    }

    @Test
    void onlyTheAuthorChangesThePublication() {
        Publication publication = draftMentoring(1);
        addSlot(publication, 0);

        assertThatThrownBy(() -> publication.publish(CLIENT, NOW)).isExactlyInstanceOf(PermissionDeniedException.class)
                .hasMessage("Only the author can publish a publication");
        assertThatThrownBy(() -> publication.edit(CLIENT, completeDetails(2)))
                .isExactlyInstanceOf(PermissionDeniedException.class)
                .hasMessage("Only the author can edit a publication");
        assertThatThrownBy(() -> publication.addSlot(CLIENT, SLOT_START.plusSeconds(86400),
                SLOT_START.plusSeconds(90000))).isExactlyInstanceOf(PermissionDeniedException.class)
                .hasMessage("Only the author can add a slot to a publication");
        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.DRAFT);
    }

    // --- Edición y franjas ---

    @Test
    void theAuthorEditsADraftAndManagesItsSlots() {
        Publication publication = draftMentoring(1);
        TimeSlot first = addSlot(publication, 0);
        addSlot(publication, 2);

        publication.edit(AUTHOR, completeDetails(3));
        publication.removeSlot(AUTHOR, first);

        assertThat(publication.getDetails().capacity()).isEqualTo(3);
        assertThat(publication.getSlots()).hasSize(1).doesNotContain(first);
    }

    @Test
    void slotsCannotOverlapNorBeRemovedIfTheyDoNotBelong() {
        Publication publication = draftMentoring(1);
        addSlot(publication, 0);

        assertThatThrownBy(() -> publication.addSlot(AUTHOR, SLOT_START.plus(Duration.ofMinutes(30)),
                SLOT_START.plus(Duration.ofMinutes(90)))).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot overlaps another slot of the publication");
        TimeSlot foreign = new TimeSlot(null, SLOT_START.plus(Duration.ofDays(5)),
                SLOT_START.plus(Duration.ofDays(5)).plus(Duration.ofHours(1)));
        assertThatThrownBy(() -> publication.removeSlot(AUTHOR, foreign)).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot does not belong to the publication");
    }

    @Test
    void aSlotMustEndAfterItStarts() {
        assertThatThrownBy(() -> new TimeSlot(null, SLOT_START, SLOT_START))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Visibilidad y solicitudes ---

    @Test
    void onlyAPublishedPublicationIsVisibleToEveryone() {
        Publication publication = draftMentoring(1);
        addSlot(publication, 0);
        assertThat(publication.isVisibleTo(CLIENT)).isFalse();
        assertThat(publication.isVisibleTo(AUTHOR)).isTrue();

        publication.publish(AUTHOR, NOW);
        assertThat(publication.isVisibleTo(CLIENT)).isTrue();

        publication.pause(AUTHOR, NOW);
        assertThat(publication.isVisibleTo(CLIENT)).isFalse();
        assertThat(publication.isVisibleTo(AUTHOR)).isTrue();
    }

    // --- Cupo ---

    @Test
    void reservingOccupiesTheSlotAndOneSpot() {
        Publication publication = published(2, 2);
        TimeSlot slot = publication.getSlots().get(0);

        publication.reserve(slot);

        assertThat(publication.getSlots().get(0).isOccupied()).isTrue();
        assertThat(publication.getReservedSpots()).isEqualTo(1);
        assertThat(publication.availableSpots()).isEqualTo(1);
    }

    @Test
    void anOccupiedSlotCannotBeReservedAgain() {
        Publication publication = published(2, 2);
        TimeSlot slot = publication.getSlots().get(0);
        publication.reserve(slot);

        assertThatThrownBy(() -> publication.reserve(slot)).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot is already taken");
    }

    @Test
    void theCapacityCannotBeExceededEvenWithFreeSlots() {
        Publication publication = published(1, 2);
        publication.reserve(publication.getSlots().get(0));
        TimeSlot freeSlot = publication.getSlots().get(1);

        assertThatThrownBy(() -> publication.reserve(freeSlot))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The publication has no capacity left");
        assertThat(freeSlot.isOccupied()).isFalse();
    }

    @Test
    void releasingFreesTheSlotAndTheSpot() {
        Publication publication = published(1, 1);
        TimeSlot slot = publication.getSlots().get(0);
        publication.reserve(slot);

        publication.release(slot);

        assertThat(slot.isOccupied()).isFalse();
        assertThat(publication.availableSpots()).isEqualTo(1);
        assertThatThrownBy(() -> publication.release(slot)).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot is not occupied");
    }

    @Test
    void aSlotIsFoundByItsScheduleEvenIfItIsAnotherInstance() {
        Publication publication = published(1, 1);
        TimeSlot own = publication.getSlots().get(0);
        TimeSlot copy = new TimeSlot(7L, own.getStartsAt(), own.getEndsAt());

        publication.reserve(copy);

        assertThat(own.isOccupied()).isTrue();
    }

    @Test
    void aPausedPublicationStillReservesButAClosedOneDoesNot() {
        Publication paused = published(1, 1);
        paused.pause(AUTHOR, NOW);
        paused.reserve(paused.getSlots().get(0));
        assertThat(paused.availableSpots()).isZero();

        Publication closed = published(1, 1);
        closed.close(AUTHOR, NOW);
        assertThatThrownBy(() -> closed.reserve(closed.getSlots().get(0))).isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The publication does not allow reservations in state CLOSED");
    }

    @Test
    void aClosedPublicationStillReleasesASpot() {
        Publication publication = published(1, 1);
        TimeSlot slot = publication.getSlots().get(0);
        publication.reserve(slot);
        publication.close(AUTHOR, NOW);

        publication.release(slot);

        assertThat(slot.isOccupied()).isFalse();
    }

    @Test
    void moreOccupiedSlotsThanCapacityStillBlockReservations() {
        List<StateTransition<PublicationStatus>> history = List.of(
                new StateTransition<>(null, PublicationStatus.DRAFT, AUTHOR, NOW, null),
                new StateTransition<>(PublicationStatus.DRAFT, PublicationStatus.PUBLISHED, AUTHOR, NOW, null));
        Instant day = SLOT_START;
        Publication inconsistent = Publication.restore(1L, AUTHOR, PublicationType.MENTORING, completeDetails(1),
                List.of(new TimeSlot(1L, day, day.plusSeconds(3600), true),
                        new TimeSlot(2L, day.plusSeconds(7200), day.plusSeconds(10800), true),
                        new TimeSlot(3L, day.plusSeconds(14400), day.plusSeconds(18000))), history);

        assertThat(inconsistent.availableSpots()).isEqualTo(-1);
        assertThatThrownBy(() -> inconsistent.reserve(inconsistent.getSlots().get(2)))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The publication has no capacity left");
    }

    // --- Datos de la publicación ---

    @Test
    void theTagsAreTrimmedLowercasedAndDeduplicated() {
        PublicationDetails details = new PublicationDetails("t", null, 1L, Modality.REMOTE, null, 1, SkillCoins.ZERO,
                Set.of(" Java ", "java", "SPRING", " "));

        assertThat(details.tags()).containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    void textsLongerThanWhatTheDatabaseStoresAreRejected() {
        assertThatThrownBy(() -> new PublicationDetails("x".repeat(151), null, 1L, Modality.REMOTE, null, 1,
                SkillCoins.ZERO, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The title cannot be longer than 150 characters");
        assertThatThrownBy(() -> new PublicationDetails("t", "x".repeat(2001), 1L, Modality.REMOTE, null, 1,
                SkillCoins.ZERO, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The description cannot be longer than 2000 characters");
        assertThatThrownBy(() -> new PublicationDetails("t", null, 1L, Modality.REMOTE, null, 1, SkillCoins.ZERO,
                Set.of("x".repeat(51)))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The tag cannot be longer than 50 characters");
    }

    // --- Reconstrucción ---

    @Test
    void aStoredPublicationIsRestoredWithItsStateAndOccupiedSlots() {
        List<StateTransition<PublicationStatus>> history = List.of(
                new StateTransition<>(null, PublicationStatus.DRAFT, AUTHOR, NOW, null),
                new StateTransition<>(PublicationStatus.DRAFT, PublicationStatus.PUBLISHED, AUTHOR, NOW, null));
        TimeSlot occupied = new TimeSlot(1L, SLOT_START, SLOT_START.plus(Duration.ofHours(1)), true);
        TimeSlot free = new TimeSlot(2L, SLOT_START.plus(Duration.ofHours(2)), SLOT_START.plus(Duration.ofHours(3)));

        Publication publication = Publication.restore(10L, AUTHOR, PublicationType.MENTORING, completeDetails(2),
                List.of(occupied, free), history);

        assertThat(publication.getId()).isEqualTo(10L);
        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(publication.getReservedSpots()).isEqualTo(1);
        assertThat(publication.acceptsRequests()).isTrue();
        assertThatThrownBy(() -> publication.publish(AUTHOR, NOW)).isInstanceOf(InvalidTransitionException.class);
    }
}
