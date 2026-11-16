package com.uade.skillswap.controllers.profile;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.profile.CreateProfileCommand;
import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.dto.profile.PublicProfileView;
import com.uade.skillswap.application.dto.profile.UpdateProfileCommand;
import com.uade.skillswap.application.ports.in.profile.CreateProfileUseCase;
import com.uade.skillswap.application.ports.in.profile.DeactivateProfileUseCase;
import com.uade.skillswap.application.ports.in.profile.GetPublicProfileUseCase;
import com.uade.skillswap.application.ports.in.profile.ManageSkillsUseCase;
import com.uade.skillswap.application.ports.in.profile.UpdateProfileUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.common.ControllerSupport;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Objects;

public class ProfileControllerImpl implements ProfileController {

    private final CreateProfileUseCase createProfile;
    private final UpdateProfileUseCase updateProfile;
    private final ManageSkillsUseCase manageSkills;
    private final DeactivateProfileUseCase deactivateProfile;
    private final GetPublicProfileUseCase getPublicProfile;
    private final IdentityService identityService;

    public ProfileControllerImpl(CreateProfileUseCase createProfile,
                                 UpdateProfileUseCase updateProfile,
                                 ManageSkillsUseCase manageSkills,
                                 DeactivateProfileUseCase deactivateProfile,
                                 GetPublicProfileUseCase getPublicProfile,
                                 IdentityService identityService) {
        this.createProfile = Objects.requireNonNull(createProfile, "createProfile must not be null");
        this.updateProfile = Objects.requireNonNull(updateProfile, "updateProfile must not be null");
        this.manageSkills = Objects.requireNonNull(manageSkills, "manageSkills must not be null");
        this.deactivateProfile = Objects.requireNonNull(deactivateProfile, "deactivateProfile must not be null");
        this.getPublicProfile = Objects.requireNonNull(getPublicProfile, "getPublicProfile must not be null");
        this.identityService = Objects.requireNonNull(identityService, "identityService must not be null");
    }

    @Override
    public ControllerResult<ProfileView> createProfile(String userId, CreateProfileForm form) {
        return ControllerResult.of(() -> {
            UserId actor = currentUser(userId, "create profile");
            if (form == null) {
                throw new IllegalArgumentException("The profile form is required");
            }
            AccountType accountType = ControllerSupport.requireEnum(
                    AccountType.class, form.accountType(), "accountType");
            ZoneId timeZone = parseOptionalZone(form.timeZone());
            return createProfile.create(new CreateProfileCommand(
                    actor,
                    form.displayName(),
                    form.bio(),
                    timeZone,
                    accountType
            ));
        });
    }

    @Override
    public ControllerResult<ProfileView> updateProfile(String userId, UpdateProfileForm form) {
        return ControllerResult.of(() -> {
            UserId actor = currentUser(userId, "update profile details");
            if (form == null) {
                throw new IllegalArgumentException("The profile update form is required");
            }
            ZoneId timeZone = parseOptionalZone(form.timeZone());
            return updateProfile.updateDetails(new UpdateProfileCommand(
                    actor,
                    form.displayName(),
                    form.bio(),
                    timeZone,
                    form.availability()
            ));
        });
    }

    @Override
    public ControllerResult<ProfileView> updateAvailability(String userId, String availability) {
        return ControllerResult.of(() -> updateProfile.updateAvailability(
                currentUser(userId, "update profile availability"), availability));
    }

    @Override
    public ControllerResult<ProfileView> addSkill(String userId, long skillId) {
        return ControllerResult.of(() -> manageSkills.addSkill(currentUser(userId, "add skill"), skillId));
    }

    @Override
    public ControllerResult<ProfileView> removeSkill(String userId, long skillId) {
        return ControllerResult.of(() -> manageSkills.removeSkill(currentUser(userId, "remove skill"), skillId));
    }

    @Override
    public ControllerResult<ProfileView> addInterest(String userId, String interest) {
        return ControllerResult.of(() -> manageSkills.addInterest(currentUser(userId, "add interest"), interest));
    }

    @Override
    public ControllerResult<ProfileView> removeInterest(String userId, String interest) {
        return ControllerResult.of(() -> manageSkills.removeInterest(currentUser(userId, "remove interest"), interest));
    }

    @Override
    public ControllerResult<ProfileView> deactivate(String userId) {
        return ControllerResult.of(() -> deactivateProfile.deactivate(currentUser(userId, "deactivate profile")));
    }

    @Override
    public ControllerResult<ProfileView> reactivate(String userId) {
        return ControllerResult.of(() -> deactivateProfile.reactivate(currentUser(userId, "reactivate profile")));
    }

    @Override
    public ControllerResult<PublicProfileView> getPublicProfile(String targetUserId, int page, int size) {
        return ControllerResult.of(() -> {
            if (targetUserId == null || targetUserId.isBlank()) {
                throw new IllegalArgumentException("The targetUserId is required");
            }
            return getPublicProfile.getPublicProfile(new UserId(targetUserId), new PageRequest(page, size));
        });
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

    private static ZoneId parseOptionalZone(String rawZone) {
        if (rawZone == null || rawZone.isBlank()) {
            return null;
        }
        try {
            return ZoneId.of(rawZone.strip());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timeZone: " + rawZone);
        }
    }
}
