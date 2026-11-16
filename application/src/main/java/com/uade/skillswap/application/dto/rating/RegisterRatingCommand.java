package com.uade.skillswap.application.dto.rating;

import com.uade.skillswap.domain.common.UserId;

public record RegisterRatingCommand(
        UserId authorId,
        long bookingId,
        UserId recipientId,
        int score,
        String comment) {
}
