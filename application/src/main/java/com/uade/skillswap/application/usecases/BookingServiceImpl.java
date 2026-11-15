package com.uade.skillswap.application.usecases;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.RequestBookingCommand;
import com.uade.skillswap.application.ports.in.BookingService;
import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.application.ports.out.BookingPayments;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookings;
    private final PublicationCatalog publications;
    private final PublicationAvailability availability;
    private final BookingPayments payments;
    private final UnitOfWork unitOfWork;

    public BookingServiceImpl(BookingRepository bookings, PublicationCatalog publications,
                              PublicationAvailability availability, BookingPayments payments, UnitOfWork unitOfWork) {
        this.bookings = Objects.requireNonNull(bookings);
        this.publications = Objects.requireNonNull(publications);
        this.availability = Objects.requireNonNull(availability);
        this.payments = Objects.requireNonNull(payments);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
    }

    @Override
    public BookingView request(RequestBookingCommand command) {
        return unitOfWork.execute(() -> {
            BookablePublication publication = findPublication(command.publicationId());
            if (!publication.published()) {
                throw new BusinessRuleException("A publication that is not published does not accept requests");
            }
            TimeSlot slot = findSlot(publication, command.slotId());
            Booking booking = Booking.request(publication.id(), command.clientId(), publication.authorId(), slot,
                    command.message(), command.goal(), publication.price());
            return BookingView.from(bookings.save(booking));
        });
    }

    // Aceptar y reservar el cupo van juntos: si no hay lugar, no queda nada aceptado.
    @Override
    public BookingView accept(Long bookingId, UserId actor) {
        return change(bookingId, booking -> {
            booking.accept(actor);
            availability.reserve(booking.getPublicationId(), booking.getSlot().getId());
        });
    }

    @Override
    public BookingView reject(Long bookingId, UserId actor, String reason) {
        return change(bookingId, booking -> booking.reject(actor, reason));
    }

    @Override
    public BookingView start(Long bookingId, UserId actor) {
        return change(bookingId, booking -> booking.start(actor));
    }

    @Override
    public BookingView complete(Long bookingId, UserId actor) {
        return change(bookingId, booking -> booking.complete(actor));
    }

    // Primero valida el estado y quién paga; si el saldo no alcanza, se revierte todo y no cambia nada (caso 5).
    @Override
    public BookingView pay(Long bookingId, UserId actor) {
        return change(bookingId, booking -> {
            booking.confirmPayment(actor);
            payments.pay(bookingId, booking.getClientId(), booking.getProviderId(), booking.getPrice());
        });
    }

    // Cancelar libera el cupo; si ya estaba pagada, además devuelve el reintegro que corresponda.
    @Override
    public BookingView cancel(Long bookingId, UserId actor, String reason) {
        return change(bookingId, booking -> {
            boolean paid = booking.getStatus() == BookingStatus.CONFIRMED;
            booking.cancel(actor, reason);
            availability.release(booking.getPublicationId(), booking.getSlot().getId());
            if (paid) {
                payments.refund(bookingId, booking.getClientId(), booking.getProviderId(), booking.getPrice(),
                        booking.getSlot().getStartsAt());
            }
        });
    }

    @Override
    public BookingView proposeReschedule(Long bookingId, UserId actor, long newSlotId) {
        return change(bookingId, booking -> {
            TimeSlot newSlot = findSlot(findPublication(booking.getPublicationId()), newSlotId);
            booking.proposeReschedule(actor, newSlot);
        });
    }

    // Primero se libera la franja anterior: con el cupo lleno, reservar antes fallaría aunque solo se mueva de franja.
    @Override
    public BookingView acceptReschedule(Long bookingId, UserId actor) {
        return change(bookingId, booking -> {
            long previousSlotId = booking.getSlot().getId();
            booking.acceptReschedule(actor);
            availability.release(booking.getPublicationId(), previousSlotId);
            availability.reserve(booking.getPublicationId(), booking.getSlot().getId());
        });
    }

    @Override
    public BookingView rejectReschedule(Long bookingId, UserId actor) {
        return change(bookingId, booking -> booking.rejectReschedule(actor));
    }

    @Override
    public List<BookingHistoryEntry> history(Long bookingId, UserId actor) {
        Booking booking = findBooking(bookingId);
        if (!booking.isParticipant(actor)) {
            throw new AccessDeniedException(actor, "view the history of booking " + bookingId);
        }
        return booking.getHistory().stream().map(BookingHistoryEntry::from).toList();
    }

    @Override
    public Page<BookingView> listAsClient(UserId actor, PageRequest page) {
        return bookings.findByClient(actor, page).map(BookingView::from);
    }

    @Override
    public Page<BookingView> listAsProvider(UserId actor, PageRequest page) {
        return bookings.findByProvider(actor, page).map(BookingView::from);
    }

    // Busca la reserva, aplica el cambio y la guarda, todo en una misma transacción.
    private BookingView change(Long bookingId, Consumer<Booking> action) {
        return unitOfWork.execute(() -> {
            Booking booking = findBooking(bookingId);
            action.accept(booking);
            return BookingView.from(bookings.save(booking));
        });
    }

    private Booking findBooking(Long bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    private BookablePublication findPublication(long publicationId) {
        return publications.findBookable(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication", publicationId));
    }

    private TimeSlot findSlot(BookablePublication publication, long slotId) {
        return publication.findSlot(slotId)
                .orElseThrow(() -> new BusinessRuleException("The slot does not belong to the publication"));
    }
}
