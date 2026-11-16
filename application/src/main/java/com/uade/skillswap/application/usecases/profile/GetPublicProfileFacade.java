package com.uade.skillswap.application.usecases.profile;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.profile.PublicProfileView;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.ports.in.profile.GetPublicProfileUseCase;
import com.uade.skillswap.application.ports.out.profile.ActivePublicationsQuery;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.application.ports.out.rating.RatingRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.InvalidProfileException;
import com.uade.skillswap.domain.profile.Profile;

import java.util.Objects;

public class GetPublicProfileFacade implements GetPublicProfileUseCase {

    private final ProfileRepository profileRepository;
    private final ActivePublicationsQuery activePublicationsQuery;
    private final RatingRepository ratingRepository;

    public GetPublicProfileFacade(ProfileRepository profileRepository,
                                  ActivePublicationsQuery activePublicationsQuery,
                                  RatingRepository ratingRepository) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository must not be null");
        this.activePublicationsQuery = Objects.requireNonNull(
                activePublicationsQuery, "activePublicationsQuery must not be null");
        this.ratingRepository = Objects.requireNonNull(ratingRepository, "ratingRepository must not be null");
    }

    @Override
    public PublicProfileView getPublicProfile(UserId targetUserId,
                                              PageRequest publicationsPage,
                                              PageRequest ratingsPage) {
        if (targetUserId == null) {
            throw new InvalidProfileException("The target userId cannot be null");
        }
        PageRequest safePublicationsPage = publicationsPage != null ? publicationsPage : PageRequest.first();
        PageRequest safeRatingsPage = ratingsPage != null ? ratingsPage : PageRequest.first();
        Profile profile = profileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", targetUserId.value()));
        Page<PublicationView> activePublications = activePublicationsQuery.findActiveByUserId(
                targetUserId, safePublicationsPage);
        Page<RatingView> recentRatings = ratingRepository.findByRecipientId(targetUserId, safeRatingsPage)
                .map(RatingView::from);
        return PublicProfileView.from(profile.toPublicSummary(), activePublications, recentRatings);
    }
}
