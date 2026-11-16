package com.uade.skillswap.application.usecases.profile;

import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.ports.in.profile.DeactivateProfileUseCase;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.Profile;

public class DeactivateProfileService implements DeactivateProfileUseCase {

    private final ProfileOperationSupport support;

    public DeactivateProfileService(ProfileRepository profileRepository, UnitOfWork unitOfWork) {
        this.support = new ProfileOperationSupport(profileRepository, unitOfWork);
    }

    @Override
    public ProfileView deactivate(UserId userId) {
        return support.mutateOwnedProfile(userId, Profile::deactivate);
    }

    @Override
    public ProfileView reactivate(UserId userId) {
        return support.mutateOwnedProfile(userId, Profile::reactivate);
    }
}
