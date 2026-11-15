package com.uade.skillswap.application.ports.out.profile;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.Profile;

import java.util.Optional;

public interface ProfileRepository {

    Profile save(Profile profile);

    Optional<Profile> findByUserId(UserId userId);

    boolean existsByUserId(UserId userId);
}
