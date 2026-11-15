package com.uade.skillswap.application.ports.in.booking;

public interface GetBookingReceiptUseCase {

    BookingReceipt getReceipt(GetBookingReceiptCommand command);
}
