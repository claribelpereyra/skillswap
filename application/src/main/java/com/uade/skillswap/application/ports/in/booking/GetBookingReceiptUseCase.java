package com.uade.skillswap.application.ports.in.booking;

import com.uade.skillswap.application.dto.booking.BookingReceipt;
import com.uade.skillswap.application.dto.booking.GetBookingReceiptCommand;

public interface GetBookingReceiptUseCase {

    BookingReceipt getReceipt(GetBookingReceiptCommand command);
}
