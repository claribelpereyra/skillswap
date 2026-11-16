package com.uade.skillswap.application.usecases.profile;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.InvalidProfileException;
import com.uade.skillswap.domain.profile.Profile;

import java.util.Objects;
import java.util.function.Consumer;

final class ProfileOperationSupport {

    private final ProfileRepository profileRepository;
    private final UnitOfWork unitOfWork;

    ProfileOperationSupport(ProfileRepository profileRepository, UnitOfWork unitOfWork) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
    }

    ProfileView mutateOwnedProfile(UserId userId, Consumer<Profile> mutation) {
        if (userId == null) {
            throw new InvalidProfileException("The userId cannot be null");
        }
        return unitOfWork.execute(() -> {
            Profile profile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Profile", userId.value()));
            mutation.accept(profile);
            return ProfileView.from(profileRepository.save(profile));
        });
    }
}
