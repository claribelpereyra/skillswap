package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaPublicationRepositoryTest extends PersistenceTestBase {

    private static final UserId AUTHOR = new UserId("ana");
    private static final UserId OTHER = new UserId("bruno");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-16T15:00:00Z");

    private final JpaPublicationRepository repository = new JpaPublicationRepository();

    @BeforeEach
    void catalog() {
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)", 1, null, "Tecnología", "CATEGORY");
    }

    private static PublicationDetails details(int capacity, String... tags) {
        return new PublicationDetails("Mock interview", "Practice a technical interview", 1L, Modality.REMOTE,
                Duration.ofMinutes(60), capacity, new SkillCoins(100), Set.of(tags));
    }

    private static Publication draft(UserId author, int capacity, int slots) {
        Publication publication = Publication.draft(author, AccountType.PROFESSIONAL, PublicationType.MENTORING,
                details(capacity, "java", "interviews"), NOW);
        for (int i = 0; i < slots; i++) {
            Instant start = MONDAY.plus(Duration.ofHours(2L * i));
            publication.addSlot(author, start, start.plus(Duration.ofHours(1)));
        }
        return publication;
    }

    private Publication save(Publication publication) {
        return unitOfWork.execute(() -> repository.save(publication));
    }

    private Publication load(long id) {
        return repository.findById(id).orElseThrow();
    }

    @Test
    void aNewDraftIsSavedWithItsSlotsTagsAndHistory() {
        Publication saved = save(draft(AUTHOR, 2, 2));

        Publication loaded = load(saved.getId());

        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getAuthorId()).isEqualTo(AUTHOR);
        assertThat(loaded.getType()).isEqualTo(PublicationType.MENTORING);
        assertThat(loaded.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(loaded.getDetails()).isEqualTo(details(2, "java", "interviews"));
        assertThat(loaded.getSlots()).hasSize(2).allSatisfy(slot -> {
            assertThat(slot.getId()).isNotNull();
            assertThat(slot.isOccupied()).isFalse();
        });
        assertThat(loaded.getHistory()).hasSize(1);
        assertThat(loaded.getHistory().get(0).occurredAt()).isEqualTo(NOW);
    }

    @Test
    void anUnknownIdIsEmpty() {
        assertThat(repository.findById(999)).isEmpty();
    }

    @Test
    void twoRootCategoriesCannotHaveTheSameName() {
        assertThatThrownBy(() -> sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)",
                2, null, "Tecnología", "CATEGORY")).isInstanceOf(PersistenceException.class);
    }

    @Test
    void savingAPublicationThatNoLongerExistsIsNotFound() {
        Publication saved = save(draft(AUTHOR, 1, 1));
        sql("DELETE FROM publication_transition WHERE publication_id = ?", saved.getId());
        sql("DELETE FROM publication WHERE id = ?", saved.getId());

        assertThatThrownBy(() -> save(saved)).isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publication not found with id " + saved.getId());
    }

    @Test
    void theTransitionsAreAppendedToTheHistoryAndTheStatusIsStored() {
        long id = save(draft(AUTHOR, 1, 1)).getId();
        Instant later = NOW.plus(Duration.ofDays(1));

        unitOfWork.execute(() -> {
            Publication publication = load(id);
            publication.publish(AUTHOR, NOW);
            publication.pause(AUTHOR, later);
            return repository.save(publication);
        });
        unitOfWork.execute(() -> {
            Publication publication = load(id);
            publication.cancel(AUTHOR, "Moving abroad", later);
            return repository.save(publication);
        });

        Publication loaded = load(id);
        assertThat(loaded.getStatus()).isEqualTo(PublicationStatus.CANCELLED);
        assertThat(loaded.getHistory()).extracting(t -> t.to()).containsExactly(PublicationStatus.DRAFT,
                PublicationStatus.PUBLISHED, PublicationStatus.PAUSED, PublicationStatus.CANCELLED);
        assertThat(loaded.getHistory().get(3).reason()).isEqualTo("Moving abroad");
        assertThat(count("publication_transition")).isEqualTo(4);
        String storedStatus = inTransaction(em -> (String) em.createNativeQuery(
                "SELECT status FROM publication WHERE id = " + id).getSingleResult());
        assertThat(storedStatus).isEqualTo("CANCELLED");
    }

    @Test
    void editingADraftReplacesDetailsTagsAndSlots() {
        Publication saved = save(draft(AUTHOR, 1, 2));
        TimeSlot first = saved.getSlots().get(0);
        Instant friday = MONDAY.plus(Duration.ofDays(4));

        unitOfWork.execute(() -> {
            Publication publication = load(saved.getId());
            publication.edit(AUTHOR, details(3, "java", "spring"));
            publication.removeSlot(AUTHOR, first);
            publication.addSlot(AUTHOR, friday, friday.plus(Duration.ofHours(1)));
            return repository.save(publication);
        });

        Publication loaded = load(saved.getId());
        assertThat(loaded.getDetails().capacity()).isEqualTo(3);
        assertThat(loaded.getDetails().tags()).containsExactlyInAnyOrder("java", "spring");
        assertThat(loaded.getSlots()).extracting(TimeSlot::getStartsAt)
                .containsExactly(MONDAY.plus(Duration.ofHours(2)), friday);
        assertThat(count("publication_slot")).isEqualTo(2);
    }

    @Test
    void reservingASlotIsStored() {
        Publication saved = save(published(1, 1));

        unitOfWork.execute(() -> {
            Publication publication = load(saved.getId());
            publication.reserve(publication.getSlots().get(0));
            return repository.save(publication);
        });

        Publication loaded = load(saved.getId());
        assertThat(loaded.getSlots().get(0).isOccupied()).isTrue();
        assertThat(loaded.availableSpots()).isZero();
    }

    // Base del caso de aceptación 4: dos aceptaciones simultáneas no pueden tomar el último lugar.
    @Test
    void twoConcurrentReservationsCannotBothTakeTheLastSpot() throws Exception {
        long id = save(published(1, 2)).getId();
        CountDownLatch bothLoaded = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int slotIndex = i;
            results.add(pool.submit(() -> {
                try {
                    unitOfWork.execute(() -> {
                        Publication publication = load(id);
                        bothLoaded.countDown();
                        await(bothLoaded);
                        publication.reserve(publication.getSlots().get(slotIndex));
                        return repository.save(publication);
                    });
                    return true;
                } catch (ConcurrentUpdateException conflict) {
                    return false;
                }
            }));
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        long succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        assertThat(succeeded).isEqualTo(1);
        Publication loaded = load(id);
        assertThat(loaded.getReservedSpots()).isEqualTo(1);
        assertThat(loaded.availableSpots()).isZero();
    }

    @Test
    void theAuthorListsTheirPublicationsPaginatedAndByStatus() {
        long first = save(draft(AUTHOR, 1, 1)).getId();
        long second = save(published(1, 1)).getId();
        long third = save(draft(AUTHOR, 1, 1)).getId();
        save(draft(OTHER, 1, 1));

        Page<Publication> firstPage = repository.findByAuthor(AUTHOR, null, new PageRequest(0, 2));
        Page<Publication> secondPage = repository.findByAuthor(AUTHOR, null, new PageRequest(1, 2));
        Page<Publication> published = repository.findByAuthor(AUTHOR, PublicationStatus.PUBLISHED, PageRequest.first());

        assertThat(firstPage.items()).extracting(Publication::getId).containsExactly(third, second);
        assertThat(firstPage.totalItems()).isEqualTo(3);
        assertThat(secondPage.items()).extracting(Publication::getId).containsExactly(first);
        assertThat(published.items()).extracting(Publication::getId).containsExactly(second);
        assertThat(published.items().get(0).getSlots()).hasSize(1);
    }

    private static Publication published(int capacity, int slots) {
        Publication publication = draft(AUTHOR, capacity, slots);
        publication.publish(AUTHOR, NOW);
        return publication;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The other transaction never loaded the publication");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
