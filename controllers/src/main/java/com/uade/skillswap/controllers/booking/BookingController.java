package com.uade.skillswap.controllers.booking;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.controllers.ControllerResult;

import java.util.List;

// Lo que conoce la interfaz: datos simples de entrada y resultados uniformes de salida.
public interface BookingController {

    ControllerResult<BookingView> request(String userId, long publicationId, long slotId, String message, String goal);

    ControllerResult<BookingView> accept(String userId, long bookingId);

    ControllerResult<BookingView> reject(String userId, long bookingId, String reason);

    ControllerResult<BookingView> start(String userId, long bookingId);

    ControllerResult<BookingView> complete(String userId, long bookingId);

    ControllerResult<BookingView> proposeReschedule(String userId, long bookingId, long newSlotId);

    ControllerResult<BookingView> acceptReschedule(String userId, long bookingId);

    ControllerResult<BookingView> rejectReschedule(String userId, long bookingId);

    ControllerResult<List<BookingHistoryEntry>> history(String userId, long bookingId);

    ControllerResult<Page<BookingView>> listAsClient(String userId, int page, int size);

    ControllerResult<Page<BookingView>> listAsProvider(String userId, int page, int size);
}
