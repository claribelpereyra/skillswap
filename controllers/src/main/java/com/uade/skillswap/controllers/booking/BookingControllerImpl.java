package com.uade.skillswap.controllers.booking;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.RequestBookingCommand;
import com.uade.skillswap.application.ports.in.BookingService;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.domain.common.UserId;

import java.util.List;
import java.util.Objects;

// Controller delgado: convierte la entrada, delega en el servicio y devuelve un resultado uniforme.
public class BookingControllerImpl implements BookingController {

    private final BookingService service;
    private final IdentityService identity;

    public BookingControllerImpl(BookingService service, IdentityService identity) {
        this.service = Objects.requireNonNull(service);
        this.identity = Objects.requireNonNull(identity);
    }

    @Override
    public ControllerResult<BookingView> request(String userId, long publicationId, long slotId,
                                                 String message, String goal) {
        return ControllerResult.of(() -> service.request(new RequestBookingCommand(
                currentUser(userId, "request a publication"), publicationId, slotId, message, goal)));
    }

    @Override
    public ControllerResult<BookingView> accept(String userId, long bookingId) {
        return ControllerResult.of(() -> service.accept(bookingId, currentUser(userId, "accept a booking")));
    }

    @Override
    public ControllerResult<BookingView> reject(String userId, long bookingId, String reason) {
        return ControllerResult.of(() -> service.reject(bookingId, currentUser(userId, "reject a booking"), reason));
    }

    @Override
    public ControllerResult<BookingView> start(String userId, long bookingId) {
        return ControllerResult.of(() -> service.start(bookingId, currentUser(userId, "start a booking")));
    }

    @Override
    public ControllerResult<BookingView> complete(String userId, long bookingId) {
        return ControllerResult.of(() -> service.complete(bookingId, currentUser(userId, "complete a booking")));
    }

    @Override
    public ControllerResult<BookingView> pay(String userId, long bookingId) {
        return ControllerResult.of(() -> service.pay(bookingId, currentUser(userId, "pay a booking")));
    }

    @Override
    public ControllerResult<BookingView> cancel(String userId, long bookingId, String reason) {
        return ControllerResult.of(() -> service.cancel(bookingId, currentUser(userId, "cancel a booking"), reason));
    }

    @Override
    public ControllerResult<BookingView> proposeReschedule(String userId, long bookingId, long newSlotId) {
        return ControllerResult.of(() -> service.proposeReschedule(bookingId,
                currentUser(userId, "reschedule a booking"), newSlotId));
    }

    @Override
    public ControllerResult<BookingView> acceptReschedule(String userId, long bookingId) {
        return ControllerResult.of(() -> service.acceptReschedule(bookingId,
                currentUser(userId, "accept a reschedule")));
    }

    @Override
    public ControllerResult<BookingView> rejectReschedule(String userId, long bookingId) {
        return ControllerResult.of(() -> service.rejectReschedule(bookingId,
                currentUser(userId, "reject a reschedule")));
    }

    @Override
    public ControllerResult<List<BookingHistoryEntry>> history(String userId, long bookingId) {
        return ControllerResult.of(() -> service.history(bookingId, currentUser(userId, "view a booking history")));
    }

    @Override
    public ControllerResult<Page<BookingView>> listAsClient(String userId, int page, int size) {
        return ControllerResult.of(() -> service.listAsClient(currentUser(userId, "list bookings"),
                new PageRequest(page, size)));
    }

    @Override
    public ControllerResult<Page<BookingView>> listAsProvider(String userId, int page, int size) {
        return ControllerResult.of(() -> service.listAsProvider(currentUser(userId, "list bookings"),
                new PageRequest(page, size)));
    }

    private UserId currentUser(String userId, String operation) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("No active user");
        }
        UserId user = new UserId(userId);
        if (!user.equals(identity.currentUserId())) {
            throw new AccessDeniedException(user, operation);
        }
        return user;
    }
}
