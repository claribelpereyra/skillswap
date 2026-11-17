package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.profile.JpaActivePublicationsQuery;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaCrossModuleQueriesTest extends PersistenceTestBase {
    private static final Instant AT = Instant.parse("2026-11-16T10:00:00Z");

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");
    private static final UserId CARLA = new UserId("carla");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant SLOT_START = Instant.parse("2026-11-20T15:00:00Z");

    private final JpaPublicationRepository publicationRepository = new JpaPublicationRepository();
    private final JpaBookingRepository bookingRepository = new JpaBookingRepository();

    @BeforeEach
    void seedCategory() {
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)", 1, null, "Tecnologia", "CATEGORY");
    }

    @Test
    void queriesPublishedPublicationsAndBookingEligibilityThroughSharedRepositories() {
        Publication firstPublished = createPublishedPublication("Java Mentoring");
        Publication pausedPublication = createPublishedPublication("Paused Session");
        pausedPublication.pause(ANA, NOW.plusSeconds(10));
        Publication secondPublished = createPublishedPublication("Testing Workshop");

        Publication savedFirst = savePublication(firstPublished);
        savePublication(pausedPublication);
        Publication savedSecond = savePublication(secondPublished);

        Booking completedBooking = Booking.request(savedFirst.getId(), ANA, BRUNO,
                savedFirst.getSlots().get(0),
                "Hola Bruno", "Mentoria", new SkillCoins(100), AT);
        completedBooking.accept(BRUNO, AT);
        completedBooking.confirmPayment(ANA, AT);
        completedBooking.start(BRUNO, AT);
        completedBooking.complete(ANA, AT);
        Booking savedCompleted = unitOfWork.execute(() -> bookingRepository.save(completedBooking));

        Booking confirmedBooking = Booking.request(savedSecond.getId(), ANA, CARLA,
                savedSecond.getSlots().get(0),
                "Hola Carla", "Code review", new SkillCoins(100), AT);
        confirmedBooking.accept(CARLA, AT);
        confirmedBooking.confirmPayment(ANA, AT);
        Booking savedConfirmed = unitOfWork.execute(() -> bookingRepository.save(confirmedBooking));

        JpaActivePublicationsQuery publicationsQuery = new JpaActivePublicationsQuery(publicationRepository);

        Page<PublicationView> anaPage = publicationsQuery.findActiveByUserId(ANA, PageRequest.first());
        assertThat(anaPage.totalItems()).isEqualTo(2L);
        assertThat(anaPage.items()).extracting(PublicationView::title)
                .containsExactlyInAnyOrder("Java Mentoring", "Testing Workshop");
        assertThat(publicationsQuery.findActiveByUserId(BRUNO, PageRequest.first()).items()).isEmpty();
        assertThat(publicationsQuery.findActiveByUserId(null, PageRequest.first()).items()).isEmpty();

        Booking loadedCompleted = bookingRepository.findById(savedCompleted.getId()).orElseThrow();
        assertThat(loadedCompleted.ratingRecipientFor(ANA)).isEqualTo(BRUNO);
        assertThat(loadedCompleted.ratingRecipientFor(BRUNO)).isEqualTo(ANA);

        Booking loadedConfirmed = bookingRepository.findById(savedConfirmed.getId()).orElseThrow();
        assertThatThrownBy(() -> loadedConfirmed.ratingRecipientFor(ANA))
                .isInstanceOf(InvalidTransitionException.class);

        assertThat(bookingRepository.findById(9999L)).isEmpty();
    }

    @Test
    void migracionesDeEsquemaNoCarganDatosDeEjemplo() {
        String demoUrl = "jdbc:h2:mem:schema-without-demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        PersistenceConfiguration demoDb = new PersistenceConfiguration(demoUrl, "sa", "");
        demoDb.migrate();
        demoDb.migrate();
        try (var emf = demoDb.createEntityManagerFactory(); var em = emf.createEntityManager()) {
            long profiles = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM profile").getSingleResult())
                    .longValue();
            long ratings = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM rating").getSingleResult())
                    .longValue();
            long publications = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM publication").getSingleResult())
                    .longValue();
            assertThat(profiles).isZero();
            assertThat(ratings).isZero();
            assertThat(publications).isZero();
        }
    }

    private Publication savePublication(Publication publication) {
        return unitOfWork.execute(() -> publicationRepository.save(publication));
    }

    private static Publication createPublishedPublication(String title) {
        PublicationDetails details = new PublicationDetails(
                title, "Practical mentoring", 1L, Modality.REMOTE, Duration.ofMinutes(60),
                2, new SkillCoins(100), Set.of("java"));
        Publication publication = Publication.draft(ANA, AccountType.PROFESSIONAL, PublicationType.MENTORING, details, NOW);
        publication.addSlot(ANA, SLOT_START, SLOT_START.plus(Duration.ofHours(1)));
        publication.publish(ANA, NOW.plusSeconds(1));
        return publication;
    }
}
