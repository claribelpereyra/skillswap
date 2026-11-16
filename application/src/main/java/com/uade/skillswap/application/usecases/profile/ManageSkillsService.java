package com.uade.skillswap.application.usecases.profile;

import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.ports.in.profile.ManageSkillsUseCase;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.InvalidProfileException;

public class ManageSkillsService implements ManageSkillsUseCase {

    private final ProfileOperationSupport support;

    public ManageSkillsService(ProfileRepository profileRepository, UnitOfWork unitOfWork) {
        this.support = new ProfileOperationSupport(profileRepository, unitOfWork);
    }

    @Override
    public ProfileView addSkill(UserId userId, long skillId) {
        long validSkillId = requirePositiveSkillId(skillId);
        return support.mutateOwnedProfile(userId, profile -> profile.addSkill(validSkillId));
    }

    @Override
    public ProfileView removeSkill(UserId userId, long skillId) {
        long validSkillId = requirePositiveSkillId(skillId);
        return support.mutateOwnedProfile(userId, profile -> profile.removeSkill(validSkillId));
    }

    @Override
    public ProfileView addInterest(UserId userId, String interest) {
        String validInterest = requireNonBlankInterest(interest);
        return support.mutateOwnedProfile(userId, profile -> profile.addInterest(validInterest));
    }

    @Override
    public ProfileView removeInterest(UserId userId, String interest) {
        String validInterest = requireNonBlankInterest(interest);
        return support.mutateOwnedProfile(userId, profile -> profile.removeInterest(validInterest));
    }

    private static long requirePositiveSkillId(long skillId) {
        if (skillId <= 0) {
            throw new InvalidProfileException("The skillId must be positive");
        }
        return skillId;
    }

    private static String requireNonBlankInterest(String interest) {
        if (interest == null || interest.isBlank()) {
            throw new InvalidProfileException("The interest cannot be null or blank");
        }
        return interest;
    }
}
