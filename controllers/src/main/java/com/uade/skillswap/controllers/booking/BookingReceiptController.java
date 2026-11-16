package com.uade.skillswap.controllers.booking;

import com.uade.skillswap.application.dto.booking.BookingReceipt;
import com.uade.skillswap.controllers.ControllerResult;

public interface BookingReceiptController {
    ControllerResult<BookingReceipt> getReceipt(long bookingId);
}
