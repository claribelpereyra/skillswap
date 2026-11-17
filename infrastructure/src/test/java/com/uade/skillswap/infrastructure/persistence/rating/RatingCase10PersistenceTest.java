package com.uade.skillswap.infrastructure.persistence.rating;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.dto.rating.RegisterRatingCommand;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.application.ports.out.rating.RatingRepository;
import com.uade.skillswap.application.usecases.rating.RegisterRatingService;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.PermissionDeniedException;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.Profile;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.rating.InvalidRatingException;
import com.uade.skillswap.domain.rating.Rating;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.profile.JpaProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingCase10PersistenceTest extends PersistenceTestBase {

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");
    private static final UserId CARLA = new UserId("carla");
    private static final TimeSlot SLOT = new TimeSlot(
            1L, Instant.parse("2026-11-20T14:00:00Z"), Instant.parse("2026-11-20T15:00:00Z"));

    private final JpaProfileRepository profileRepository = new JpaProfileRepository();
    private final JpaRatingRepository ratingRepository = new JpaRatingRepository();
    private final JpaBookingRepository bookingRepository = new JpaBookingRepository();

    private RegisterRatingService registerService;
    private long firstCompletedBookingId;
    private long secondCompletedBookingId;
    private long confirmedBookingId;
    private long missingRecipientBookingId;

    @BeforeEach
    void setUpCase10Context() {
        sql("INSERT INTO publication (id, author_id, type, status, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                1L, "bruno", "MENTORING", "PUBLISHED", 4, SLOT.getStartsAt());
        sql("INSERT INTO publication_slot (id, publication_id, starts_at, ends_at) VALUES (?, ?, ?, ?)",
                1L, 1L, SLOT.getStartsAt(), SLOT.getEndsAt());
        registerService = new RegisterRatingService(
                ratingRepository,
                profileRepository,
                bookingRepository,
                unitOfWork
        );

        unitOfWork.execute(() -> {
            profileRepository.save(Profile.create(ANA, "Ana", "Java mentor", ZoneId.of("UTC"), AccountType.PROFESSIONAL));
            profileRepository.save(Profile.create(BRUNO, "Bruno", "Frontend dev", ZoneId.of("UTC"), AccountType.PROFESSIONAL));
        });

        firstCompletedBookingId = createBooking(ANA, BRUNO, true);
        secondCompletedBookingId = createBooking(ANA, BRUNO, true);
        confirmedBookingId = createBooking(ANA, BRUNO, false);
        missingRecipientBookingId = createBooking(ANA, CARLA, true);
    }

    @Test
    void case10EndToEndCompletedBookingRatingAndDuplicateGuardInRealDatabase() {
        RatingView first = registerService.register(
                new RegisterRatingCommand(ANA, firstCompletedBookingId, BRUNO, 5, "Excelente mentoria"));
        assertThat(first.id()).isNotNull();

        Profile brunoAfterFirst = profileRepository.findByUserId(BRUNO).orElseThrow();
        assertThat(brunoAfterFirst.getRatingCount()).isEqualTo(1);
        assertThat(brunoAfterFirst.getReputation()).isEqualTo(5.0);

        registerService.register(new RegisterRatingCommand(BRUNO, firstCompletedBookingId, ANA, 4, "Muy buena alumna"));
        Profile anaAfterRating = profileRepository.findByUserId(ANA).orElseThrow();
        assertThat(anaAfterRating.getRatingCount()).isEqualTo(1);
        assertThat(anaAfterRating.getReputation()).isEqualTo(4.0);

        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(ANA, firstCompletedBookingId, BRUNO, 1, "Duplicate")))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("Booking " + firstCompletedBookingId + " has already been rated by user ana");
        assertThat(profileRepository.findByUserId(BRUNO).orElseThrow().getRatingCount()).isEqualTo(1);

        registerService.register(new RegisterRatingCommand(ANA, secondCompletedBookingId, BRUNO, 3, "Buena"));
        Profile brunoAfterSecond = profileRepository.findByUserId(BRUNO).orElseThrow();
        assertThat(brunoAfterSecond.getRatingSum()).isEqualTo(8);
        assertThat(brunoAfterSecond.getRatingCount()).isEqualTo(2);
        assertThat(brunoAfterSecond.getReputation()).isEqualTo(4.0);

        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(ANA, confirmedBookingId, BRUNO, 5, "Early")))
                .isExactlyInstanceOf(InvalidTransitionException.class)
                .hasMessage("Cannot rate in state CONFIRMED");
        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(CARLA, firstCompletedBookingId, BRUNO, 5, "Outsider")))
                .isExactlyInstanceOf(PermissionDeniedException.class)
                .hasMessage("Only the client or the provider can rate a booking");

        Page<Rating> storedForBruno = ratingRepository.findByRecipientId(BRUNO, PageRequest.first());
        assertThat(storedForBruno.totalItems()).isEqualTo(2L);
        assertThat(count("rating")).isEqualTo(3L);
    }

    @Test
    void rollsBackTransactionWhenRecipientProfileIsMissing() {
        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(ANA, missingRecipientBookingId, CARLA, 5, "No profile")))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profile not found with id carla");

        assertThat(count("rating")).isZero();
    }

    @Test
    void detectsOptimisticLockConflictWhenTwoConcurrentRatingsUpdateSameProfile() throws Exception {
        long carlaBookingId = createBooking(CARLA, BRUNO, true);
        CountDownLatch bothLoadedProfile = new CountDownLatch(2);
        ProfileRepository synchronizedProfileRepo = new ProfileRepository() {
            @Override
            public Profile save(Profile profile) {
                return profileRepository.save(profile);
            }

            @Override
            public Optional<Profile> findByUserId(UserId userId) {
                Optional<Profile> found = profileRepository.findByUserId(userId);
                bothLoadedProfile.countDown();
                await(bothLoadedProfile);
                return found;
            }

            @Override
            public boolean existsByUserId(UserId userId) {
                return profileRepository.existsByUserId(userId);
            }
        };
        RegisterRatingService synchronizedService = new RegisterRatingService(
                ratingRepository, synchronizedProfileRepo, bookingRepository, unitOfWork);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> outcomes = List.of(
                    pool.submit(() -> executeExpectingConflict(
                            synchronizedService, new RegisterRatingCommand(ANA, firstCompletedBookingId, BRUNO, 5, "Ana"))),
                    pool.submit(() -> executeExpectingConflict(
                            synchronizedService, new RegisterRatingCommand(CARLA, carlaBookingId, BRUNO, 3, "Carla")))
            );
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

            List<String> results = List.of(outcomes.get(0).get(), outcomes.get(1).get());
            assertThat(results).containsExactlyInAnyOrder("OK", "CONFLICT");
        } finally {
            pool.shutdownNow();
        }

        Profile bruno = profileRepository.findByUserId(BRUNO).orElseThrow();
        assertThat(bruno.getRatingCount()).isEqualTo(1);
        assertThat(count("rating")).isEqualTo(1L);
    }

    @Test
    void translatesConcurrentDuplicateRatingRaceIntoBusinessRuleError() throws Exception {
        CountDownLatch bothPassedPreCheck = new CountDownLatch(2);
        RatingRepository synchronizedRatingRepo = new RatingRepository() {
            @Override
            public Rating save(Rating rating) {
                return ratingRepository.save(rating);
            }

            @Override
            public boolean existsByBookingIdAndAuthorId(long bookingId, UserId authorId) {
                boolean exists = ratingRepository.existsByBookingIdAndAuthorId(bookingId, authorId);
                bothPassedPreCheck.countDown();
                await(bothPassedPreCheck);
                return exists;
            }

            @Override
            public Page<Rating> findByRecipientId(UserId recipientId, PageRequest pageRequest) {
                return ratingRepository.findByRecipientId(recipientId, pageRequest);
            }
        };
        RegisterRatingService synchronizedService = new RegisterRatingService(
                synchronizedRatingRepo, profileRepository, bookingRepository, unitOfWork);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> outcomes = List.of(
                    pool.submit(() -> executeExpectingDuplicate(
                            synchronizedService, new RegisterRatingCommand(ANA, firstCompletedBookingId, BRUNO, 5, "First"))),
                    pool.submit(() -> executeExpectingDuplicate(
                            synchronizedService, new RegisterRatingCommand(ANA, firstCompletedBookingId, BRUNO, 4, "Second")))
            );
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

            List<String> results = List.of(outcomes.get(0).get(), outcomes.get(1).get());
            assertThat(results).containsExactlyInAnyOrder("OK", "DUPLICATE");
        } finally {
            pool.shutdownNow();
        }

        assertThat(count("rating")).isEqualTo(1L);
        assertThat(profileRepository.findByUserId(BRUNO).orElseThrow().getRatingCount()).isEqualTo(1);
    }

    private long createBooking(UserId client, UserId provider, boolean completed) {
        Booking booking = Booking.request(1L, client, provider, SLOT, "Hola", "Mentoria", new SkillCoins(100));
        booking.accept(provider);
        booking.confirmPayment(client);
        if (completed) {
            booking.start(provider);
            booking.complete(client);
        }
        return unitOfWork.execute(() -> bookingRepository.save(booking)).getId();
    }

    private static String executeExpectingConflict(RegisterRatingService service, RegisterRatingCommand command) {
        try {
            service.register(command);
            return "OK";
        } catch (ConcurrentUpdateException expected) {
            return "CONFLICT";
        }
    }

    private static String executeExpectingDuplicate(RegisterRatingService service, RegisterRatingCommand command) {
        try {
            service.register(command);
            return "OK";
        } catch (InvalidRatingException expected) {
            return "DUPLICATE";
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent worker did not reach barrier in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
