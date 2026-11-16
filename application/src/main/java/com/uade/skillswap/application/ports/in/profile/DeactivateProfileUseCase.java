package com.uade.skillswap.application.ports.in.profile;

import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.domain.common.UserId;

public interface DeactivateProfileUseCase {

    ProfileView deactivate(UserId userId);

    ProfileView reactivate(UserId userId);
}
