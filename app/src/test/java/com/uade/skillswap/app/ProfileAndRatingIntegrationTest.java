package com.uade.skillswap.app;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.dto.profile.PublicProfileView;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.application.ports.out.BookingPricingPolicy;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.usecases.BookingServiceImpl;
import com.uade.skillswap.application.usecases.profile.CreateProfileService;
import com.uade.skillswap.application.usecases.profile.DeactivateProfileService;
import com.uade.skillswap.application.usecases.profile.GetPublicProfileFacade;
import com.uade.skillswap.application.usecases.profile.ManageSkillsService;
import com.uade.skillswap.application.usecases.profile.UpdateProfileService;
import com.uade.skillswap.application.usecases.rating.ListRatingsService;
import com.uade.skillswap.application.usecases.rating.RegisterRatingService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.controllers.booking.BookingController;
import com.uade.skillswap.controllers.booking.BookingControllerImpl;
import com.uade.skillswap.controllers.profile.CreateProfileForm;
import com.uade.skillswap.controllers.profile.ProfileController;
import com.uade.skillswap.controllers.profile.ProfileControllerImpl;
import com.uade.skillswap.controllers.profile.UpdateProfileForm;
import com.uade.skillswap.controllers.rating.RatingController;
import com.uade.skillswap.controllers.rating.RatingControllerImpl;
import com.uade.skillswap.controllers.rating.RatingForm;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.wallet.FullRefundStrategy;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.infrastructure.adapters.MockIdentityAdapter;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import com.uade.skillswap.infrastructure.persistence.JpaUnitOfWork;
import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.profile.JpaActivePublicationsQuery;
import com.uade.skillswap.infrastructure.persistence.profile.JpaAuthorProfileQuery;
import com.uade.skillswap.infrastructure.persistence.profile.JpaProfileRepository;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import com.uade.skillswap.infrastructure.persistence.rating.JpaRatingRepository;
import com.uade.skillswap.infrastructure.persistence.wallet.JpaWalletRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAndRatingIntegrationTest {
    private static final Instant AT = Instant.parse("2026-11-16T10:00:00Z");

    private static final String URL = "jdbc:h2:mem:app-integration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant SLOT_START = Instant.parse("2026-11-20T15:00:00Z");

    private static JpaUnitOfWork unitOfWork;

    private MockIdentityAdapter identityAdapter;
    private JpaAuthorProfileQuery authorProfileQuery;
    private JpaBookingRepository bookingRepo;
    private JpaPublicationRepository publicationRepository;
    private ProfileController profileController;
    private BookingController bookingController;
    private RatingController ratingController;

    @BeforeAll
    static void initDatabase() {
        PersistenceConfiguration config = new PersistenceConfiguration(URL, "sa", "");
        config.migrate();
        EntityManagerFactory factory = config.createEntityManagerFactory();
        EntityManagerContext.init(factory);
        unitOfWork = new JpaUnitOfWork();
    }

    @AfterAll
    static void closeDatabase() {
        EntityManagerContext.close();
    }

    @BeforeEach
    void setUpWiringAndCleanTables() {
        unitOfWork.execute(() -> EntityManagerContext.withEntityManager(em -> {
            @SuppressWarnings("unchecked")
            List<String> tables = em.createNativeQuery("""
                            SELECT table_name FROM information_schema.tables
                            WHERE table_schema = CURRENT_SCHEMA AND table_type = 'BASE TABLE'
                              AND table_name <> 'flyway_schema_history'
                            """)
                    .getResultList();
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            try {
                for (String table : tables) {
                    em.createNativeQuery("TRUNCATE TABLE " + table + " RESTART IDENTITY").executeUpdate();
                }
            } finally {
                em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            }
            return null;
        }));

        identityAdapter = new MockIdentityAdapter();
        JpaProfileRepository profileRepo = new JpaProfileRepository();
        JpaRatingRepository ratingRepo = new JpaRatingRepository();
        bookingRepo = new JpaBookingRepository();
        publicationRepository = new JpaPublicationRepository();
        authorProfileQuery = new JpaAuthorProfileQuery();
        JpaActivePublicationsQuery activePublicationsQuery = new JpaActivePublicationsQuery(publicationRepository);

        PublicationCatalog catalog = publicationId -> publicationRepository.findById(publicationId)
                .map(p -> new BookablePublication(
                        p.getId(),
                        p.getAuthorId(),
                        p.getStatus() == PublicationStatus.PUBLISHED,
                        p.getDetails().basePrice(),
                        p.getSlots()));
        PublicationAvailability availability = new PublicationAvailability() {
            @Override
            public void reserve(long publicationId, long slotId) {
                Publication publication = publicationRepository.findById(publicationId).orElseThrow();
                TimeSlot slot = publication.getSlots().stream()
                        .filter(s -> s.getId() == slotId)
                        .findFirst()
                        .orElseThrow();
                publication.reserve(slot);
                publicationRepository.save(publication);
            }

            @Override
            public void release(long publicationId, long slotId) {
                Publication publication = publicationRepository.findById(publicationId).orElseThrow();
                TimeSlot slot = publication.getSlots().stream()
                        .filter(s -> s.getId() == slotId)
                        .findFirst()
                        .orElseThrow();
                publication.release(slot);
                publicationRepository.save(publication);
            }
        };

        profileController = new ProfileControllerImpl(
                new CreateProfileService(profileRepo, unitOfWork, java.time.Clock.systemUTC()),
                new UpdateProfileService(profileRepo, unitOfWork),
                new ManageSkillsService(profileRepo, unitOfWork),
                new DeactivateProfileService(profileRepo, unitOfWork),
                new GetPublicProfileFacade(profileRepo, activePublicationsQuery, ratingRepo),
                identityAdapter
        );
        bookingController = new BookingControllerImpl(
                new BookingServiceImpl(
                        bookingRepo, catalog, availability,
                        new JpaWalletRepository(),
                        BookingPricingPolicy.basePrice(),
                        new FullRefundStrategy(),
                        unitOfWork,
                        Clock.systemUTC(), ids -> java.util.Map.of()),
                identityAdapter
        );
        ratingController = new RatingControllerImpl(
                new RegisterRatingService(ratingRepo, profileRepo, bookingRepo, unitOfWork, java.time.Clock.systemUTC()),
                new ListRatingsService(ratingRepo, profileRepo),
                identityAdapter
        );
    }

    @Test
    void executesFullProfileLifecycleAndVerifiedRatingEndToEnd() {
        identityAdapter.signIn(new UserId("ana"));
        ControllerResult<ProfileView> anaCreated = profileController.createProfile("ana",
                new CreateProfileForm("Ana Perez", "Java mentor", "America/Argentina/Buenos_Aires", "PROFESSIONAL"));
        assertThat(anaCreated.isSuccess()).isTrue();
        profileController.addSkill("ana", 1L);
        profileController.addInterest("ana", "Architecture");
        profileController.updateProfile("ana",
                new UpdateProfileForm("Ana Perez", "Senior Java Mentor", "UTC", "Weekdays 18-21"));

        identityAdapter.signIn(new UserId("techlab"));
        assertThat(profileController.createProfile("techlab",
                new CreateProfileForm("TechLab UADE", "Workshops", "UTC", "ORGANIZATION")).isSuccess()).isTrue();

        assertThat(authorProfileQuery.accountTypeOf(new UserId("ana"))).contains(AccountType.PROFESSIONAL);
        assertThat(authorProfileQuery.accountTypeOf(new UserId("techlab"))).contains(AccountType.ORGANIZATION);

        ControllerResult<ProfileView> unauthorizedEdit = profileController.addSkill("ana", 2L);
        assertThat(unauthorizedEdit.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(unauthorizedEdit.getMessage()).isEqualTo("User ana is not allowed to add skill");

        unitOfWork.execute(() -> EntityManagerContext.withEntityManager(em ->
                em.createNativeQuery("INSERT INTO category (id, parent_id, name, kind) VALUES (1, NULL, 'Tech', 'CATEGORY')")
                        .executeUpdate()));
        Publication mentoring = createPublishedPublication("ana", "Hexagonal Java Mentoring");
        Publication codeReview = createPublishedPublication("ana", "TDD Code Review");
        long slotId = mentoring.getSlots().get(0).getId();

        identityAdapter.signIn(new UserId("techlab"));
        ControllerResult<BookingView> requested = bookingController.request(
                "techlab", mentoring.getId(), slotId, "Hola Ana", "Taller");
        assertThat(requested.isSuccess()).isTrue();
        long bookingId = requested.getData().id();

        identityAdapter.signIn(new UserId("ana"));
        assertThat(bookingController.accept("ana", bookingId).isSuccess()).isTrue();

        // El módulo de billetera/pagos confirma el pago directamente sobre Booking hasta integrarse en BookingService.
        identityAdapter.signIn(new UserId("techlab"));
        Booking confirmed = unitOfWork.execute(() -> {
            Booking booking = bookingRepo.findById(bookingId).orElseThrow();
            booking.confirmPayment(new UserId("techlab"), AT);
            return bookingRepo.save(booking);
        });
        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        ControllerResult<RatingView> beforeCompletion = ratingController.registerRating(
                "techlab", new RatingForm(bookingId, "ana", 5, "Too early"));
        assertThat(beforeCompletion.getErrorType()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(beforeCompletion.getMessage()).isEqualTo("Cannot rate in state CONFIRMED");

        identityAdapter.signIn(new UserId("ana"));
        assertThat(bookingController.start("ana", bookingId).getData().status())
                .isEqualTo(BookingStatus.IN_PROGRESS);

        identityAdapter.signIn(new UserId("techlab"));
        assertThat(bookingController.complete("techlab", bookingId).getData().status())
                .isEqualTo(BookingStatus.COMPLETED);

        ControllerResult<RatingView> rated = ratingController.registerRating("techlab",
                new RatingForm(bookingId, "ana", 5, "Excelente taller"));
        assertThat(rated.isSuccess()).isTrue();

        ControllerResult<RatingView> duplicate = ratingController.registerRating("techlab",
                new RatingForm(bookingId, "ana", 2, "Repeated"));
        assertThat(duplicate.getErrorType()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(duplicate.getMessage()).isEqualTo("Booking " + bookingId + " has already been rated by user techlab");

        identityAdapter.signOut();
        ControllerResult<RatingView> loggedOut = ratingController.registerRating(
                "techlab", new RatingForm(bookingId, "ana", 5, "No auth"));
        assertThat(loggedOut.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);

        ControllerResult<PublicProfileView> publicView = profileController.getPublicProfile("ana", 0, 10);
        assertThat(publicView.isSuccess()).isTrue();
        assertThat(publicView.getData().reputation()).isEqualTo(5.0);
        assertThat(publicView.getData().ratingCount()).isEqualTo(1);
        assertThat(publicView.getData().skillIds()).containsExactly(1L);
        assertThat(publicView.getData().activePublications().totalItems()).isEqualTo(2L);
        assertThat(publicView.getData().activePublications().items())
                .extracting(view -> view.title())
                .containsExactly(codeReview.getDetails().title(), mentoring.getDetails().title());
        assertThat(publicView.getData().recentRatings().items()).hasSize(1);

        ControllerResult<Page<RatingView>> ratingsPage = ratingController.listReceivedRatings("ana", 0, 10);
        assertThat(ratingsPage.isSuccess()).isTrue();
        assertThat(ratingsPage.getData().totalItems()).isEqualTo(1L);

        identityAdapter.signIn(new UserId("ana"));
        assertThat(profileController.deactivate("ana").getData().active()).isFalse();
        assertThat(authorProfileQuery.accountTypeOf(new UserId("ana"))).isEmpty();
        assertThat(profileController.addSkill("ana", 3L).getErrorType()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(ratingController.listReceivedRatings("ana", 0, 10).getData().totalItems()).isEqualTo(1L);
    }

    private Publication createPublishedPublication(String authorId, String title) {
        UserId author = new UserId(authorId);
        AccountType accountType = Optional.ofNullable(authorId)
                .map(UserId::new)
                .flatMap(authorProfileQuery::accountTypeOf)
                .orElse(AccountType.PROFESSIONAL);
        PublicationDetails details = new PublicationDetails(
                title, "Practical session", 1L, Modality.REMOTE, Duration.ofMinutes(60),
                2, new SkillCoins(100), Set.of("java"));
        Publication publication = Publication.draft(author, accountType, PublicationType.MENTORING, details, NOW);
        publication.addSlot(author, SLOT_START, SLOT_START.plus(Duration.ofHours(1)));
        publication.publish(author, NOW.plusSeconds(5));
        return unitOfWork.execute(() -> publicationRepository.save(publication));
    }
}
