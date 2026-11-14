package com.uade.skillswap.controllers.booking;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.RequestBookingCommand;
import com.uade.skillswap.application.ports.in.BookingService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.domain.common.UserId;

import java.util.List;
import java.util.Objects;

// Controller delgado: convierte la entrada, delega en el servicio y devuelve un resultado uniforme.
public class BookingControllerImpl implements BookingController {

    private final BookingService service;

    public BookingControllerImpl(BookingService service) {
        this.service = Objects.requireNonNull(service);
    }

    @Override
    public ControllerResult<BookingView> request(String userId, long publicationId, long slotId,
                                                 String message, String goal) {
        return ControllerResult.of(() -> service.request(
                new RequestBookingCommand(new UserId(userId), publicationId, slotId, message, goal)));
    }

    @Override
    public ControllerResult<BookingView> accept(String userId, long bookingId) {
        return ControllerResult.of(() -> service.accept(bookingId, new UserId(userId)));
    }

    @Override
    public ControllerResult<BookingView> reject(String userId, long bookingId, String reason) {
        return ControllerResult.of(() -> service.reject(bookingId, new UserId(userId), reason));
    }

    @Override
    public ControllerResult<BookingView> start(String userId, long bookingId) {
        return ControllerResult.of(() -> service.start(bookingId, new UserId(userId)));
    }

    @Override
    public ControllerResult<BookingView> complete(String userId, long bookingId) {
        return ControllerResult.of(() -> service.complete(bookingId, new UserId(userId)));
    }

    @Override
    public ControllerResult<BookingView> proposeReschedule(String userId, long bookingId, long newSlotId) {
        return ControllerResult.of(() -> service.proposeReschedule(bookingId, new UserId(userId), newSlotId));
    }

    @Override
    public ControllerResult<BookingView> acceptReschedule(String userId, long bookingId) {
        return ControllerResult.of(() -> service.acceptReschedule(bookingId, new UserId(userId)));
    }

    @Override
    public ControllerResult<BookingView> rejectReschedule(String userId, long bookingId) {
        return ControllerResult.of(() -> service.rejectReschedule(bookingId, new UserId(userId)));
    }

    @Override
    public ControllerResult<List<BookingHistoryEntry>> history(String userId, long bookingId) {
        return ControllerResult.of(() -> service.history(bookingId, new UserId(userId)));
    }

    @Override
    public ControllerResult<Page<BookingView>> listAsClient(String userId, int page, int size) {
        return ControllerResult.of(() -> service.listAsClient(new UserId(userId), new PageRequest(page, size)));
    }

    @Override
    public ControllerResult<Page<BookingView>> listAsProvider(String userId, int page, int size) {
        return ControllerResult.of(() -> service.listAsProvider(new UserId(userId), new PageRequest(page, size)));
    }
}
