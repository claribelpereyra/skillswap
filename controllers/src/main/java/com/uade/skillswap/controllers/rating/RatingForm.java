package com.uade.skillswap.controllers.rating;

public record RatingForm(
        long bookingId,
        String recipientId,
        int score,
        String comment) {
}
