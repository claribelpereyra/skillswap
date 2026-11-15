package com.uade.skillswap.application.ports.out.wallet;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

public interface SkillCoinGateway {

    boolean authorizeCredit(UserId userId, SkillCoins amount);
}
