package com.uade.skillswap.application.ports.in.wallet;

import com.uade.skillswap.domain.common.SkillCoins;

public interface CancelBookingUseCase {

    SkillCoins cancel(CancelBookingCommand command);
}
