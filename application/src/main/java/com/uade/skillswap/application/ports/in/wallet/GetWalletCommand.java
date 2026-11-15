package com.uade.skillswap.application.ports.in.wallet;

import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public record GetWalletCommand(UserId actorId, UserId ownerId) {

    public GetWalletCommand {
        Objects.requireNonNull(actorId, "The requesting user is required");
        Objects.requireNonNull(ownerId, "The wallet owner is required");
    }
}
