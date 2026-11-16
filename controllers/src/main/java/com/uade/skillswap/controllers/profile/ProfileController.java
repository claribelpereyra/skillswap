package com.uade.skillswap.controllers.profile;

import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.dto.profile.PublicProfileView;
import com.uade.skillswap.controllers.ControllerResult;

public interface ProfileController {

    ControllerResult<ProfileView> createProfile(String userId, CreateProfileForm form);

    ControllerResult<ProfileView> updateProfile(String userId, UpdateProfileForm form);

    ControllerResult<ProfileView> updateAvailability(String userId, String availability);

    ControllerResult<ProfileView> addSkill(String userId, long skillId);

    ControllerResult<ProfileView> removeSkill(String userId, long skillId);

    ControllerResult<ProfileView> addInterest(String userId, String interest);

    ControllerResult<ProfileView> removeInterest(String userId, String interest);

    ControllerResult<ProfileView> deactivate(String userId);

    ControllerResult<ProfileView> reactivate(String userId);

    ControllerResult<PublicProfileView> getPublicProfile(String targetUserId, int page, int size);
}
