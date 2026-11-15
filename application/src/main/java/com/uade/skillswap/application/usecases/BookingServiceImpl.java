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
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.observer.BookingObserver;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookings;
    private final PublicationCatalog publications;
    private final PublicationAvailability availability;
    private final UnitOfWork unitOfWork;
    private final List<BookingObserver> observers = new ArrayList<>();

    public BookingServiceImpl(BookingRepository bookings, PublicationCatalog publications,
                              PublicationAvailability availability, UnitOfWork unitOfWork) {
        this.bookings = Objects.requireNonNull(bookings);
        this.publications = Objects.requireNonNull(publications);
        this.availability = Objects.requireNonNull(availability);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
    }

    // Los observers los registra el composition root; se les avisa recién cuando la transacción se confirmó.
    public void addObserver(BookingObserver observer) {
        observers.add(Objects.requireNonNull(observer));
    }

    @Override
    public BookingView request(RequestBookingCommand command) {
        Changed changed = unitOfWork.execute(() -> {
            BookablePublication publication = findPublication(command.publicationId());
            if (!publication.published()) {
                throw new BusinessRuleException("A publication that is not published does not accept requests");
            }
            TimeSlot slot = findSlot(publication, command.slotId());
            Booking booking = Booking.request(publication.id(), command.clientId(), publication.authorId(), slot,
                    command.message(), command.goal(), publication.price());
            return new Changed(booking, bookings.save(booking));
        });
        return publish(changed);
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
        Changed changed = unitOfWork.execute(() -> {
            Booking booking = findBooking(bookingId);
            action.accept(booking);
            return new Changed(booking, bookings.save(booking));
        });
        return publish(changed);
    }

    // Si la transacción falla no se llega acá, así que nadie se entera de un cambio que no quedó guardado.
    private BookingView publish(Changed changed) {
        observers.forEach(changed.changed()::addObserver);
        changed.changed().publishPendingEvents(changed.saved().getId());
        return BookingView.from(changed.saved());
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

    // El repositorio devuelve una copia nueva con id; las transiciones sin avisar quedan en la instancia que se modificó.
    private record Changed(Booking changed, Booking saved) {
    }
}
