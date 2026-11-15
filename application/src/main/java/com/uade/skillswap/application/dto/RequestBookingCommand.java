package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.common.UserId;

public record RequestBookingCommand(UserId clientId, long publicationId, long slotId, String message, String goal) {
}
