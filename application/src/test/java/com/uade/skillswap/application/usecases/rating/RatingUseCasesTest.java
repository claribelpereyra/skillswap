package com.uade.skillswap.application.usecases.rating;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.dto.rating.RegisterRatingCommand;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.application.ports.out.rating.EligibleBookingQuery;
import com.uade.skillswap.application.ports.out.rating.EligibleBookingQuery.BookingTarget;
import com.uade.skillswap.application.ports.out.rating.RatingRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.Profile;
import com.uade.skillswap.domain.rating.InvalidRatingException;
import com.uade.skillswap.domain.rating.Rating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingUseCasesTest {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final UserId OUTSIDER = new UserId("carla");
    private static final long BOOKING_ID = 10L;

    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private EligibleBookingQuery eligibleBookingQuery;

    private final UnitOfWork immediateUnitOfWork = new UnitOfWork() {
        @Override
        public <T> T execute(Supplier<T> work) {
            return work.get();
        }
    };

    private RegisterRatingService registerService;
    private ListRatingsService listService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterRatingService(
                ratingRepository, profileRepository, eligibleBookingQuery, immediateUnitOfWork);
        listService = new ListRatingsService(ratingRepository, profileRepository);

        lenient().when(ratingRepository.save(any(Rating.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(profileRepository.save(any(Profile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registersRatingAndUpdatesCounterpartyReputationAtomically() {
        Profile recipientProfile = Profile.rebuild(2L, PROVIDER, "Bruno", "Bio", ZoneId.of("UTC"),
                AccountType.PROFESSIONAL, 4, 1, Set.of(), Set.of(), null, true,
                Instant.parse("2026-11-01T10:00:00Z"));
        when(eligibleBookingQuery.findBooking(BOOKING_ID))
                .thenReturn(Optional.of(new BookingTarget(BOOKING_ID, CLIENT, PROVIDER, true)));
        when(ratingRepository.existsByBookingIdAndAuthorId(BOOKING_ID, CLIENT)).thenReturn(false);
        when(profileRepository.findByUserId(PROVIDER)).thenReturn(Optional.of(recipientProfile));

        RatingView view = registerService.register(
                new RegisterRatingCommand(CLIENT, BOOKING_ID, PROVIDER, 5, "Great!"));

        assertThat(view.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(view.authorId()).isEqualTo("ana");
        assertThat(view.recipientId()).isEqualTo("bruno");
        assertThat(view.score()).isEqualTo(5);
        assertThat(recipientProfile.getRatingCount()).isEqualTo(2);
        assertThat(recipientProfile.getReputation()).isEqualTo(4.5);
        verify(ratingRepository).save(any(Rating.class));
        verify(profileRepository).save(recipientProfile);
    }

    @Test
    void rejectsNullCommandNullAuthorAndSelfRating() {
        assertThatThrownBy(() -> registerService.register(null))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The rating command cannot be null");

        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(null, BOOKING_ID, PROVIDER, 5, "ok")))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The authorId cannot be null");

        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(CLIENT, BOOKING_ID, CLIENT, 5, "ok")))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("A user cannot rate themselves");
    }

    @Test
    void rejectsMissingIncompleteNonParticipantWrongCounterpartyDuplicateAndMissingProfile() {
        when(eligibleBookingQuery.findBooking(BOOKING_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(CLIENT, BOOKING_ID, PROVIDER, 5, "ok")))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found with id 10");

        when(eligibleBookingQuery.findBooking(BOOKING_ID))
                .thenReturn(Optional.of(new BookingTarget(BOOKING_ID, CLIENT, PROVIDER, false)));
        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(CLIENT, BOOKING_ID, PROVIDER, 5, "ok")))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("Only completed bookings can be rated");

        when(eligibleBookingQuery.findBooking(BOOKING_ID))
                .thenReturn(Optional.of(new BookingTarget(BOOKING_ID, CLIENT, PROVIDER, true)));
        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(OUTSIDER, BOOKING_ID, PROVIDER, 5, "ok")))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessage("User carla is not allowed to rate booking 10");

        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(PROVIDER, BOOKING_ID, null, 5, "ok")))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The rating recipient cannot be null");
        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(PROVIDER, BOOKING_ID, OUTSIDER, 5, "ok")))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The recipient does not match the counterparty of booking 10");

        when(ratingRepository.existsByBookingIdAndAuthorId(BOOKING_ID, PROVIDER)).thenReturn(true);
        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(PROVIDER, BOOKING_ID, CLIENT, 5, "ok")))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("Booking 10 has already been rated by user bruno");

        when(ratingRepository.existsByBookingIdAndAuthorId(BOOKING_ID, PROVIDER)).thenReturn(false);
        when(profileRepository.findByUserId(CLIENT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> registerService.register(
                new RegisterRatingCommand(PROVIDER, BOOKING_ID, CLIENT, 5, "ok")))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profile not found with id ana");

        verify(ratingRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void bookingTargetCounterpartyHelperHandlesAllParticipantCases() {
        BookingTarget target = new BookingTarget(BOOKING_ID, CLIENT, PROVIDER, true);

        assertThat(target.involves(null)).isFalse();
        assertThat(target.involves(OUTSIDER)).isFalse();
        assertThat(target.involves(CLIENT)).isTrue();
        assertThat(target.counterpartyOf(null)).isEmpty();
        assertThat(target.counterpartyOf(OUTSIDER)).isEmpty();
        assertThat(target.counterpartyOf(CLIENT)).contains(PROVIDER);
        assertThat(target.counterpartyOf(PROVIDER)).contains(CLIENT);
    }

    @Test
    void listsPaginatedRatingsForExistingProfileAndRejectsMissingOrNullRecipient() {
        Rating rating = Rating.rebuild(1L, BOOKING_ID, CLIENT, PROVIDER, 4, "Good",
                Instant.parse("2026-11-10T10:00:00Z"));
        when(profileRepository.existsByUserId(PROVIDER)).thenReturn(true);
        when(ratingRepository.findByRecipientId(PROVIDER, PageRequest.first()))
                .thenReturn(new Page<>(List.of(rating), PageRequest.first(), 1));

        Page<RatingView> page = listService.listReceivedRatings(PROVIDER, null);
        assertThat(page.items()).singleElement().satisfies(r -> assertThat(r.score()).isEqualTo(4));

        assertThatThrownBy(() -> listService.listReceivedRatings(null, PageRequest.first()))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The recipientId cannot be null");

        when(profileRepository.existsByUserId(OUTSIDER)).thenReturn(false);
        assertThatThrownBy(() -> listService.listReceivedRatings(OUTSIDER, PageRequest.first()))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profile not found with id carla");
    }
}
