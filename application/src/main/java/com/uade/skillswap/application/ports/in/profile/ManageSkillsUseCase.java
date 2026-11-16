package com.uade.skillswap.application.ports.in.profile;

import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.domain.common.UserId;

public interface ManageSkillsUseCase {

    ProfileView addSkill(UserId userId, long skillId);

    ProfileView removeSkill(UserId userId, long skillId);

    ProfileView addInterest(UserId userId, String interest);

    ProfileView removeInterest(UserId userId, String interest);
}
