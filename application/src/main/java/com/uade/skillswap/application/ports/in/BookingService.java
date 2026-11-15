package com.uade.skillswap.application.ports.in;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.RequestBookingCommand;
import com.uade.skillswap.domain.common.UserId;

import java.util.List;

public interface BookingService {

    BookingView request(RequestBookingCommand command);

    BookingView accept(Long bookingId, UserId actor);

    BookingView reject(Long bookingId, UserId actor, String reason);

    BookingView start(Long bookingId, UserId actor);

    BookingView complete(Long bookingId, UserId actor);

    BookingView pay(Long bookingId, UserId actor);

    BookingView cancel(Long bookingId, UserId actor, String reason);

    BookingView proposeReschedule(Long bookingId, UserId actor, long newSlotId);

    BookingView acceptReschedule(Long bookingId, UserId actor);

    BookingView rejectReschedule(Long bookingId, UserId actor);

    List<BookingHistoryEntry> history(Long bookingId, UserId actor);

    Page<BookingView> listAsClient(UserId actor, PageRequest page);

    Page<BookingView> listAsProvider(UserId actor, PageRequest page);
}
