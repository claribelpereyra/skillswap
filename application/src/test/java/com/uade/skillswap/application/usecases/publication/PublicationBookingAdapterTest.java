package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.publication.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicationBookingAdapterTest {

    private static final UserId AUTHOR = new UserId("ana");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-16T15:00:00Z");
    private static final long SLOT_A = 11;
    private static final long SLOT_B = 12;

    private final InMemoryPublicationRepository repository = new InMemoryPublicationRepository();
    private final PublicationBookingAdapter adapter = new PublicationBookingAdapter(repository);

    // Publicación guardada con franjas que ya tienen id, como vienen de la base.
    private long stored(PublicationStatus status, int capacity) {
        List<StateTransition<PublicationStatus>> history = new ArrayList<>();
        history.add(new StateTransition<>(null, PublicationStatus.DRAFT, AUTHOR, NOW, null));
        if (status != PublicationStatus.DRAFT) {
            history.add(new StateTransition<>(PublicationStatus.DRAFT, status, AUTHOR, NOW, null));
        }
        List<TimeSlot> slots = List.of(
                new TimeSlot(SLOT_A, MONDAY, MONDAY.plus(Duration.ofHours(1))),
                new TimeSlot(SLOT_B, MONDAY.plus(Duration.ofDays(1)), MONDAY.plus(Duration.ofDays(1)).plusSeconds(3600)));
        PublicationDetails details = new PublicationDetails("Mock interview", null, 1L, Modality.REMOTE, null, capacity,
                new SkillCoins(100), null);
        return repository.save(Publication.restore(null, AUTHOR, PublicationType.MENTORING, details, slots, history))
                .getId();
    }

    private TimeSlot slotOf(long publicationId, long slotId) {
        return repository.findById(publicationId).orElseThrow().getSlots().stream()
                .filter(s -> s.getId() == slotId).findFirst().orElseThrow();
    }

    @Test
    void aPublishedPublicationIsBookableWithItsAuthorPriceAndSlots() {
        long id = stored(PublicationStatus.PUBLISHED, 1);

        BookablePublication bookable = adapter.findBookable(id).orElseThrow();

        assertThat(bookable.id()).isEqualTo(id);
        assertThat(bookable.authorId()).isEqualTo(AUTHOR);
        assertThat(bookable.published()).isTrue();
        assertThat(bookable.price()).isEqualTo(new SkillCoins(100));
        assertThat(bookable.findSlot(SLOT_B)).isPresent();
    }

    @Test
    void aPausedOrDraftPublicationIsNotBookable() {
        assertThat(adapter.findBookable(stored(PublicationStatus.PAUSED, 1)).orElseThrow().published()).isFalse();
        assertThat(adapter.findBookable(stored(PublicationStatus.DRAFT, 1)).orElseThrow().published()).isFalse();
        assertThat(adapter.findBookable(999)).isEmpty();
    }

    @Test
    void reservingOccupiesTheSlotAndSavesThePublication() {
        long id = stored(PublicationStatus.PUBLISHED, 2);
        int savesBefore = repository.saves;

        adapter.reserve(id, SLOT_A);

        assertThat(slotOf(id, SLOT_A).isOccupied()).isTrue();
        assertThat(repository.findById(id).orElseThrow().availableSpots()).isEqualTo(1);
        assertThat(repository.saves).isEqualTo(savesBefore + 1);
    }

    @Test
    void anOccupiedSlotOrAFullPublicationCannotBeReserved() {
        long id = stored(PublicationStatus.PUBLISHED, 1);
        adapter.reserve(id, SLOT_A);

        assertThatThrownBy(() -> adapter.reserve(id, SLOT_A)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> adapter.reserve(id, SLOT_B))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("capacity");
        assertThat(slotOf(id, SLOT_B).isOccupied()).isFalse();
    }

    @Test
    void anUnknownSlotOrPublicationIsRejected() {
        long id = stored(PublicationStatus.PUBLISHED, 1);

        assertThatThrownBy(() -> adapter.reserve(id, 99)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> adapter.reserve(999, SLOT_A)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void releasingFreesTheSlotAndTheSpot() {
        long id = stored(PublicationStatus.PUBLISHED, 1);
        adapter.reserve(id, SLOT_A);

        adapter.release(id, SLOT_A);

        assertThat(slotOf(id, SLOT_A).isOccupied()).isFalse();
        assertThat(repository.findById(id).orElseThrow().availableSpots()).isEqualTo(1);
        assertThatThrownBy(() -> adapter.release(id, SLOT_A)).isInstanceOf(BusinessRuleException.class);
    }

    // Reprogramar con el cupo lleno: hay que liberar la franja vieja ANTES de reservar la nueva.
    // Al revés, la reserva falla por cupo agotado aunque la contratación solo se esté moviendo.
    @Test
    void reschedulingWithAFullCapacityNeedsToReleaseBeforeReserving() {
        long id = stored(PublicationStatus.PUBLISHED, 1);
        adapter.reserve(id, SLOT_A);

        assertThatThrownBy(() -> adapter.reserve(id, SLOT_B)).isInstanceOf(BusinessRuleException.class);

        adapter.release(id, SLOT_A);
        adapter.reserve(id, SLOT_B);
        assertThat(slotOf(id, SLOT_A).isOccupied()).isFalse();
        assertThat(slotOf(id, SLOT_B).isOccupied()).isTrue();
    }
}
