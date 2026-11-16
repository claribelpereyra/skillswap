package com.uade.skillswap.application.ports.in.profile;

import com.uade.skillswap.application.dto.profile.CreateProfileCommand;
import com.uade.skillswap.application.dto.profile.ProfileView;

public interface CreateProfileUseCase {

    ProfileView create(CreateProfileCommand command);
}
