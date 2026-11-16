package com.uade.skillswap.application.ports.out.rating;

import com.uade.skillswap.domain.common.UserId;

import java.util.Optional;

public interface EligibleBookingQuery {

    Optional<BookingTarget> findBooking(long bookingId);

    record BookingTarget(long bookingId, UserId clientId, UserId providerId, boolean completed) {

        public boolean involves(UserId userId) {
            return userId != null && (userId.equals(clientId) || userId.equals(providerId));
        }

        public Optional<UserId> counterpartyOf(UserId authorId) {
            if (authorId == null) {
                return Optional.empty();
            }
            if (authorId.equals(clientId)) {
                return Optional.ofNullable(providerId);
            }
            if (authorId.equals(providerId)) {
                return Optional.ofNullable(clientId);
            }
            return Optional.empty();
        }
    }
}
