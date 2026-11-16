package com.uade.skillswap.controllers.booking;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.dto.booking.BookingReceipt;
import com.uade.skillswap.application.dto.booking.GetBookingReceiptCommand;
import com.uade.skillswap.application.ports.in.booking.GetBookingReceiptUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import java.util.Objects;

public final class BookingReceiptControllerImpl implements BookingReceiptController {
    private final GetBookingReceiptUseCase query;
    private final IdentityService identity;

    public BookingReceiptControllerImpl(GetBookingReceiptUseCase query, IdentityService identity) {
        this.query = Objects.requireNonNull(query);
        this.identity = Objects.requireNonNull(identity);
    }

    @Override
    public ControllerResult<BookingReceipt> getReceipt(long bookingId) {
        return ControllerResult.of(() -> {
            var actor = identity.currentUserId();
            if (actor == null) throw new AccessDeniedException("No active user");
            return query.getReceipt(new GetBookingReceiptCommand(actor, bookingId));
        });
    }
}
