package com.uade.skillswap.application.usecases.profile;

import com.uade.skillswap.application.dto.profile.CreateProfileCommand;
import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.ports.in.profile.CreateProfileUseCase;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.domain.profile.InvalidProfileException;
import com.uade.skillswap.domain.profile.Profile;

import java.util.Objects;

public class CreateProfileService implements CreateProfileUseCase {

    private final ProfileRepository profileRepository;
    private final UnitOfWork unitOfWork;

    public CreateProfileService(ProfileRepository profileRepository, UnitOfWork unitOfWork) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
    }

    @Override
    public ProfileView create(CreateProfileCommand command) {
        if (command == null || command.userId() == null) {
            throw new InvalidProfileException("The userId cannot be null");
        }
        return unitOfWork.execute(() -> {
            if (profileRepository.existsByUserId(command.userId())) {
                throw new InvalidProfileException(
                        "A profile already exists for user " + command.userId().value());
            }
            Profile profile = Profile.create(
                    command.userId(),
                    command.displayName(),
                    command.bio(),
                    command.timeZone(),
                    command.accountType()
            );
            return ProfileView.from(profileRepository.save(profile));
        });
    }
}
