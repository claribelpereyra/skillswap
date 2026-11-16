package com.uade.skillswap.controllers.rating;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.dto.rating.RegisterRatingCommand;
import com.uade.skillswap.application.ports.in.rating.ListRatingsUseCase;
import com.uade.skillswap.application.ports.in.rating.RegisterRatingUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public class RatingControllerImpl implements RatingController {

    private final RegisterRatingUseCase registerRating;
    private final ListRatingsUseCase listRatings;
    private final IdentityService identityService;

    public RatingControllerImpl(RegisterRatingUseCase registerRating,
                                ListRatingsUseCase listRatings,
                                IdentityService identityService) {
        this.registerRating = Objects.requireNonNull(registerRating, "registerRating must not be null");
        this.listRatings = Objects.requireNonNull(listRatings, "listRatings must not be null");
        this.identityService = Objects.requireNonNull(identityService, "identityService must not be null");
    }

    @Override
    public ControllerResult<RatingView> registerRating(String userId, RatingForm form) {
        return ControllerResult.of(() -> {
            UserId actor = currentUser(userId, "register a rating");
            if (form == null) {
                throw new IllegalArgumentException("The rating form is required");
            }
            UserId recipient = requireUserId(form.recipientId(), "recipientId");
            return registerRating.register(new RegisterRatingCommand(
                    actor,
                    form.bookingId(),
                    recipient,
                    form.score(),
                    form.comment()
            ));
        });
    }

    @Override
    public ControllerResult<Page<RatingView>> listReceivedRatings(String recipientId, int page, int size) {
        return ControllerResult.of(() -> listRatings.listReceivedRatings(
                requireUserId(recipientId, "recipientId"),
                new PageRequest(page, size)
        ));
    }

    private UserId currentUser(String userId, String operation) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("No active user");
        }
        UserId user = new UserId(userId);
        if (!user.equals(identityService.currentUserId())) {
            throw new AccessDeniedException(user, operation);
        }
        return user;
    }

    private static UserId requireUserId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The " + field + " is required");
        }
        return new UserId(value);
    }
}
