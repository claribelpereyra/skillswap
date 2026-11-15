package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.domain.common.UserId;

public interface IdentityService {

    UserId currentUserId();
}
