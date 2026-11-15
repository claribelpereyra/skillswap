package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicationAdaptersTest extends PersistenceTestBase {

    static final UserId PROVIDER = new UserId("bruno");
    static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    static final Instant MONDAY = Instant.parse("2026-11-16T15:00:00Z");

    private final JpaPublicationRepository repository = new JpaPublicationRepository();
    private final PublicationCatalogAdapter catalog = new PublicationCatalogAdapter(repository);
    private final PublicationAvailabilityAdapter availability = new PublicationAvailabilityAdapter(repository);

    @BeforeEach
    void category() {
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)", 1, null, "Tecnología", "CATEGORY");
    }

    @Test
    void theCatalogTranslatesAPublishedPublication() {
        Publication saved = save(publication(2, 2, true));

        BookablePublication bookable = catalog.findBookable(saved.getId()).orElseThrow();

        assertThat(bookable.id()).isEqualTo(saved.getId());
        assertThat(bookable.authorId()).isEqualTo(PROVIDER);
        assertThat(bookable.published()).isTrue();
        assertThat(bookable.price()).isEqualTo(new SkillCoins(100));
        assertThat(bookable.slots()).hasSize(2).allSatisfy(slot -> assertThat(slot.getId()).isNotNull());
    }

    @Test
    void theCatalogMarksADraftAsNotPublishedAndIgnoresUnknownIds() {
        Publication draft = save(publication(1, 1, false));

        assertThat(catalog.findBookable(draft.getId())).get().extracting(BookablePublication::published).isEqualTo(false);
        assertThat(catalog.findBookable(12345L)).isEmpty();
    }

    @Test
    void reservingAndReleasingASlotIsStored() {
        Publication saved = save(publication(2, 2, true));
        long slotId = slotId(saved, 0);

        unitOfWork.execute(() -> availability.reserve(saved.getId(), slotId));
        assertThat(repository.findById(saved.getId()).orElseThrow().getReservedSpots()).isEqualTo(1);

        unitOfWork.execute(() -> availability.release(saved.getId(), slotId));
        assertThat(repository.findById(saved.getId()).orElseThrow().getReservedSpots()).isZero();
    }

    @Test
    void reservingFailsWhenThereIsNoCapacityLeft() {
        Publication saved = save(publication(1, 2, true));
        unitOfWork.execute(() -> availability.reserve(saved.getId(), slotId(saved, 0)));

        assertThatThrownBy(() -> unitOfWork.execute(() -> availability.reserve(saved.getId(), slotId(saved, 1))))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The publication has no capacity left");
    }

    @Test
    void reservingFailsForAnUnknownPublicationOrAForeignSlot() {
        Publication saved = save(publication(1, 1, true));

        assertThatThrownBy(() -> unitOfWork.execute(() -> availability.reserve(12345L, 1L)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> unitOfWork.execute(() -> availability.reserve(saved.getId(), 999L)))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot does not belong to the publication");
    }

    private Publication save(Publication publication) {
        return unitOfWork.execute(() -> repository.save(publication));
    }

    static long slotId(Publication publication, int index) {
        return publication.getSlots().stream()
                .map(TimeSlot::getId)
                .sorted()
                .toList()
                .get(index);
    }

    static Publication publication(int capacity, int slots, boolean published) {
        Publication publication = Publication.draft(PROVIDER, AccountType.PROFESSIONAL, PublicationType.MENTORING,
                new PublicationDetails("Mock interview", "Practice a technical interview", 1L, Modality.REMOTE,
                        Duration.ofMinutes(60), capacity, new SkillCoins(100), Set.of("java")),
                NOW);
        for (int i = 0; i < slots; i++) {
            Instant start = MONDAY.plus(Duration.ofHours(2L * i));
            publication.addSlot(PROVIDER, start, start.plus(Duration.ofHours(1)));
        }
        if (published) {
            publication.publish(PROVIDER, NOW);
        }
        return publication;
    }
}
