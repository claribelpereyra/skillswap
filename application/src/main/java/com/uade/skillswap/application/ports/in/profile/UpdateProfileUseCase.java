package com.uade.skillswap.application.ports.in.profile;

import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.dto.profile.UpdateProfileCommand;
import com.uade.skillswap.domain.common.UserId;

public interface UpdateProfileUseCase {

    ProfileView updateDetails(UpdateProfileCommand command);

    ProfileView updateAvailability(UserId userId, String availability);
}
