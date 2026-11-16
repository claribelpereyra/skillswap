package com.uade.skillswap.application.usecases.profile;

import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.dto.profile.UpdateProfileCommand;
import com.uade.skillswap.application.ports.in.profile.UpdateProfileUseCase;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.Availability;
import com.uade.skillswap.domain.profile.InvalidProfileException;

public class UpdateProfileService implements UpdateProfileUseCase {

    private final ProfileOperationSupport support;

    public UpdateProfileService(ProfileRepository profileRepository, UnitOfWork unitOfWork) {
        this.support = new ProfileOperationSupport(profileRepository, unitOfWork);
    }

    @Override
    public ProfileView updateDetails(UpdateProfileCommand command) {
        if (command == null) {
            throw new InvalidProfileException("The update command cannot be null");
        }
        return support.mutateOwnedProfile(command.userId(), profile -> {
            profile.updateDetails(command.displayName(), command.bio(), command.timeZone());
            if (command.availability() != null) {
                profile.updateAvailability(new Availability(command.availability()));
            }
        });
    }

    @Override
    public ProfileView updateAvailability(UserId userId, String availability) {
        return support.mutateOwnedProfile(userId,
                profile -> profile.updateAvailability(new Availability(availability)));
    }
}
