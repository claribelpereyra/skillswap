package com.uade.skillswap.application.usecases.rating;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.ports.in.rating.ListRatingsUseCase;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.application.ports.out.rating.RatingRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.InvalidRatingException;

import java.util.Objects;

public class ListRatingsService implements ListRatingsUseCase {

    private final RatingRepository ratingRepository;
    private final ProfileRepository profileRepository;

    public ListRatingsService(RatingRepository ratingRepository, ProfileRepository profileRepository) {
        this.ratingRepository = Objects.requireNonNull(ratingRepository, "ratingRepository must not be null");
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository must not be null");
    }

    @Override
    public Page<RatingView> listReceivedRatings(UserId recipientId, PageRequest pageRequest) {
        if (recipientId == null) {
            throw new InvalidRatingException("The recipientId cannot be null");
        }
        if (!profileRepository.existsByUserId(recipientId)) {
            throw new ResourceNotFoundException("Profile", recipientId.value());
        }
        PageRequest safePage = pageRequest != null ? pageRequest : PageRequest.first();
        return ratingRepository.findByRecipientId(recipientId, safePage)
                .map(RatingView::from);
    }
}
