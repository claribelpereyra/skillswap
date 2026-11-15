package com.uade.skillswap.controllers.rating;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.dto.rating.RegisterRatingCommand;
import com.uade.skillswap.application.ports.in.rating.ListRatingsUseCase;
import com.uade.skillswap.application.ports.in.rating.RegisterRatingUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.InvalidRatingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingControllerTest {

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");

    @Mock
    private RegisterRatingUseCase registerRating;
    @Mock
    private ListRatingsUseCase listRatings;
    @Mock
    private IdentityService identityService;

    private RatingController controller;

    @BeforeEach
    void setUp() {
        lenient().when(identityService.currentUserId()).thenReturn(ANA);
        controller = new RatingControllerImpl(registerRating, listRatings, identityService);
    }

    @Test
    void delegatesValidRatingRegistrationAndPaginatedHistory() {
        RatingView expected = new RatingView(1L, 10L, "ana", "bruno", 5, "Great!", Instant.now());
        when(registerRating.register(new RegisterRatingCommand(ANA, 10L, BRUNO, 5, "Great!")))
                .thenReturn(expected);
        when(listRatings.listReceivedRatings(BRUNO, new PageRequest(0, 5)))
                .thenReturn(new Page<>(List.of(expected), new PageRequest(0, 5), 1));

        ControllerResult<RatingView> registered = controller.registerRating("ana",
                new RatingForm(10L, "bruno", 5, "Great!"));
        assertThat(registered.isSuccess()).isTrue();
        assertThat(registered.getData()).isEqualTo(expected);
        assertThat(registered.getErrorType()).isNull();

        ControllerResult<Page<RatingView>> listed = controller.listReceivedRatings("bruno", 0, 5);
        assertThat(listed.isSuccess()).isTrue();
        assertThat(listed.getData().items()).containsExactly(expected);
    }

    @Test
    void returnsInvalidInputWhenRatingFormOrRecipientIsMissing() {
        ControllerResult<RatingView> nullForm = controller.registerRating("ana", null);
        ControllerResult<RatingView> blankRecipient = controller.registerRating("ana",
                new RatingForm(10L, "  ", 5, "ok"));

        assertThat(nullForm.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(nullForm.getMessage()).isEqualTo("The rating form is required");
        assertThat(blankRecipient.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(blankRecipient.getMessage()).isEqualTo("The recipientId is required");
        verifyNoInteractions(registerRating);
    }

    @Test
    void returnsInvalidInputWhenListingRatingsWithMissingRecipientOrInvalidPageSize() {
        ControllerResult<Page<RatingView>> nullRecipient = controller.listReceivedRatings(null, 0, 5);
        ControllerResult<Page<RatingView>> invalidSize = controller.listReceivedRatings("bruno", 0, 0);

        assertThat(nullRecipient.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(nullRecipient.getMessage()).isEqualTo("The recipientId is required");
        assertThat(invalidSize.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(invalidSize.getMessage()).isEqualTo("Page size must be between 1 and 100: 0");
        verifyNoInteractions(listRatings);
    }

    @Test
    void returnsAccessDeniedWhenActiveUserIsMissing() {
        ControllerResult<RatingView> result = controller.registerRating(" ", new RatingForm(10L, "bruno", 5, "ok"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(result.getMessage()).isEqualTo("No active user");
        verifyNoInteractions(registerRating);
    }

    @Test
    void returnsAccessDeniedWhenNoUserIsAuthenticatedOrActorDiffers() {
        when(identityService.currentUserId()).thenReturn(null);
        ControllerResult<RatingView> noAuth = controller.registerRating("ana", new RatingForm(10L, "bruno", 5, "ok"));

        when(identityService.currentUserId()).thenReturn(BRUNO);
        ControllerResult<RatingView> impersonated = controller.registerRating("ana",
                new RatingForm(10L, "bruno", 5, "ok"));

        assertThat(noAuth.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(noAuth.getMessage()).isEqualTo("User ana is not allowed to register a rating");
        assertThat(impersonated.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(impersonated.getMessage()).isEqualTo("User ana is not allowed to register a rating");
        verifyNoInteractions(registerRating);
    }

    @Test
    void returnsAccessDeniedWhenUserDidNotParticipateInBooking() {
        when(registerRating.register(new RegisterRatingCommand(ANA, 10L, BRUNO, 5, "ok")))
                .thenThrow(new AccessDeniedException(ANA, "rate booking 10"));

        ControllerResult<RatingView> result = controller.registerRating("ana", new RatingForm(10L, "bruno", 5, "ok"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(result.getMessage()).isEqualTo("User ana is not allowed to rate booking 10");
    }

    @Test
    void returnsNotFoundWhenBookingDoesNotExist() {
        when(registerRating.register(new RegisterRatingCommand(ANA, 99L, BRUNO, 5, "ok")))
                .thenThrow(new ResourceNotFoundException("Booking", 99L));

        ControllerResult<RatingView> result = controller.registerRating("ana", new RatingForm(99L, "bruno", 5, "ok"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.getMessage()).isEqualTo("Booking not found with id 99");
    }

    @Test
    void returnsBusinessRuleWhenDomainRejectsRating() {
        when(registerRating.register(new RegisterRatingCommand(ANA, 10L, BRUNO, 9, "ok")))
                .thenThrow(new InvalidRatingException("Invalid score"));

        ControllerResult<RatingView> result = controller.registerRating("ana", new RatingForm(10L, "bruno", 9, "ok"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(result.getMessage()).isEqualTo("Invalid score");
    }

    @Test
    void returnsConflictOnConcurrentReputationUpdateAndUnexpectedOnUnhandledError() {
        ConcurrentUpdateException conflict = new ConcurrentUpdateException(new RuntimeException("version mismatch"));
        when(registerRating.register(new RegisterRatingCommand(ANA, 10L, BRUNO, 5, "ok"))).thenThrow(conflict);
        when(listRatings.listReceivedRatings(BRUNO, new PageRequest(0, 5)))
                .thenThrow(new IllegalStateException("db down"));

        ControllerResult<RatingView> conflictResult = controller.registerRating("ana",
                new RatingForm(10L, "bruno", 5, "ok"));
        ControllerResult<Page<RatingView>> unexpectedResult = controller.listReceivedRatings("bruno", 0, 5);

        assertThat(conflictResult.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        assertThat(conflictResult.getMessage()).isEqualTo(conflict.getMessage());
        assertThat(unexpectedResult.getErrorType()).isEqualTo(ErrorType.UNEXPECTED);
        assertThat(unexpectedResult.getMessage()).isEqualTo("An unexpected error occurred");
    }
}
