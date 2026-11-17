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
import com.uade.skillswap.application.ports.out.BookingPricingPolicy;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.WalletRepository;
import com.uade.skillswap.application.ports.out.BookingLabelsQuery;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.CancellationOutcome;
import com.uade.skillswap.domain.booking.observer.BookingObserver;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.wallet.RefundStrategy;
import com.uade.skillswap.domain.wallet.Wallet;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookings;
    private final PublicationCatalog publications;
    private final PublicationAvailability availability;
    private final WalletRepository wallets;
    private final BookingPricingPolicy pricingPolicy;
    private final RefundStrategy refundStrategy;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final BookingLabelsQuery labels;
    private final List<BookingObserver> observers = new ArrayList<>();

    public BookingServiceImpl(BookingRepository bookings, PublicationCatalog publications,
                              PublicationAvailability availability, WalletRepository wallets,
                              BookingPricingPolicy pricingPolicy, RefundStrategy refundStrategy,
                              UnitOfWork unitOfWork, Clock clock, BookingLabelsQuery labels) {
        this.bookings = Objects.requireNonNull(bookings);
        this.publications = Objects.requireNonNull(publications);
        this.availability = Objects.requireNonNull(availability);
        this.wallets = Objects.requireNonNull(wallets);
        this.pricingPolicy = Objects.requireNonNull(pricingPolicy);
        this.refundStrategy = Objects.requireNonNull(refundStrategy);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
        this.labels = Objects.requireNonNull(labels);
    }

    // Los observers los registra el composition root; se les avisa recién cuando la transacción se confirmó.
    public void addObserver(BookingObserver observer) {
        observers.add(Objects.requireNonNull(observer));
    }

    @Override
    public BookingView request(RequestBookingCommand command) {
        return unitOfWork.execute(() -> {
            BookablePublication publication = findPublication(command.publicationId());
            if (!publication.published()) {
                throw new BusinessRuleException("A publication that is not published does not accept requests");
            }
            TimeSlot slot = findSlot(publication, command.slotId());
            requireFree(slot);
            SkillCoins finalPrice = Objects.requireNonNull(
                    pricingPolicy.priceFor(command.clientId(), publication),
                    "The booking price is required");
            Booking booking = Booking.request(publication.id(), command.clientId(), publication.authorId(), slot,
                    command.message(), command.goal(), finalPrice, clock.instant());
            Changed changed = new Changed(booking, bookings.save(booking));
            unitOfWork.afterCommit(() -> publish(changed));
            return withLabels(changed.saved());
        });
    }

    // Aceptar y reservar el cupo van juntos: si no hay lugar, no queda nada aceptado.
    @Override
    public BookingView accept(Long bookingId, UserId actor) {
        return change(bookingId, booking -> {
            booking.accept(actor, clock.instant());
            availability.reserve(booking.getPublicationId(), booking.getSlot().getId());
        });
    }

    @Override
    public BookingView pay(Long bookingId, UserId actor) {
        return unitOfWork.execute(() -> {
            Booking booking = findBooking(bookingId);
            if (!booking.getClientId().equals(actor)) {
                throw new AccessDeniedException(actor, "pay for booking " + bookingId);
            }
            booking.validatePayment(actor);

            if (booking.getPrice().amount() > 0) {
                Wallet client = findWallet(booking.getClientId());
                Wallet provider = findWallet(booking.getProviderId());
                client.payBookingTo(provider, booking.getPrice(), requirePersistedBookingId(booking), clock.instant());
                wallets.save(client);
                wallets.save(provider);
            }

            booking.confirmPayment(actor, clock.instant());
            return saveAndScheduleEvents(booking);
        });
    }

    @Override
    public BookingView cancel(Long bookingId, UserId actor, String reason) {
        return unitOfWork.execute(() -> {
            Booking booking = findBooking(bookingId);
            CancellationOutcome outcome = booking.cancel(actor, reason, clock.instant());

            if (outcome.refundable()) {
                SkillCoins refundAmount = refundStrategy.refundAmount(
                        booking.getPrice(), booking.getSlot().getStartsAt(), clock.instant());
                if (refundAmount.amount() > 0) {
                    Wallet client = findWallet(booking.getClientId());
                    Wallet provider = findWallet(booking.getProviderId());
                    provider.refundBookingTo(client, refundAmount, requirePersistedBookingId(booking), clock.instant());
                    wallets.save(provider);
                    wallets.save(client);
                }
            }

            if (outcome.releasesSlot()) {
                availability.release(booking.getPublicationId(), booking.getSlot().getId());
            }
            return saveAndScheduleEvents(booking);
        });
    }

    @Override
    public BookingView reject(Long bookingId, UserId actor, String reason) {
        return change(bookingId, booking -> booking.reject(actor, reason, clock.instant()));
    }

    @Override
    public BookingView start(Long bookingId, UserId actor) {
        return change(bookingId, booking -> booking.start(actor, clock.instant()));
    }

    @Override
    public BookingView complete(Long bookingId, UserId actor) {
        return change(bookingId, booking -> booking.complete(actor, clock.instant()));
    }

    @Override
    public BookingView proposeReschedule(Long bookingId, UserId actor, long newSlotId) {
        return change(bookingId, booking -> {
            TimeSlot newSlot = findSlot(findPublication(booking.getPublicationId()), newSlotId);
            requireFree(newSlot);
            booking.proposeReschedule(actor, newSlot);
        });
    }

    // Primero se libera la franja anterior: con el cupo lleno, reservar antes fallaría aunque solo se mueva de franja.
    @Override
    public BookingView acceptReschedule(Long bookingId, UserId actor) {
        return change(bookingId, booking -> {
            long previousSlotId = booking.getSlot().getId();
            booking.acceptReschedule(actor, clock.instant());
            availability.release(booking.getPublicationId(), previousSlotId);
            availability.reserve(booking.getPublicationId(), booking.getSlot().getId());
        });
    }

    @Override
    public BookingView rejectReschedule(Long bookingId, UserId actor) {
        return change(bookingId, booking -> booking.rejectReschedule(actor, clock.instant()));
    }

    @Override
    public BookingView get(Long bookingId, UserId actor) {
        Booking booking = findBooking(bookingId);
        if (!booking.isParticipant(actor)) {
            throw new AccessDeniedException(actor, "view booking " + bookingId);
        }
        return withLabels(booking);
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
        return withLabels(bookings.findByClient(actor, page));
    }

    @Override
    public Page<BookingView> listAsProvider(UserId actor, PageRequest page) {
        return withLabels(bookings.findByProvider(actor, page));
    }

    private BookingView withLabels(Booking booking) {
        var names = booking.getId() == null ? java.util.Map.<Long, BookingLabelsQuery.Labels>of()
                : labels.forBookings(java.util.Set.of(booking.getId()));
        return withLabels(booking, booking.getId() == null ? null : names.get(booking.getId()));
    }

    private static BookingView withLabels(Booking booking, BookingLabelsQuery.Labels row) {
        var view = BookingView.from(booking);
        return row == null ? view : view.withLabels(row.title(), row.clientName(), row.providerName());
    }

    private Page<BookingView> withLabels(Page<Booking> page) {
        var ids = page.items().stream().map(Booking::getId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        var names = labels.forBookings(ids);
        return page.map(booking -> withLabels(booking,
                booking.getId() == null ? null : names.get(booking.getId())));
    }

    // Busca la reserva, aplica el cambio y la guarda, todo en una misma transacción.
    private BookingView change(Long bookingId, Consumer<Booking> action) {
        return unitOfWork.execute(() -> {
            Booking booking = findBooking(bookingId);
            action.accept(booking);
            return saveAndScheduleEvents(booking);
        });
    }

    private BookingView saveAndScheduleEvents(Booking booking) {
        Changed changed = new Changed(booking, bookings.save(booking));
        unitOfWork.afterCommit(() -> publish(changed));
        return withLabels(changed.saved());
    }
    // Lo registra el execute() mediante afterCommit; si la transacción revierte, no se dispara.
    private void publish(Changed changed) {
        observers.forEach(changed.changed()::addObserver);
        changed.changed().publishPendingEvents(changed.saved().getId());
    }

    private Booking findBooking(Long bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    private Wallet findWallet(UserId ownerId) {
        return wallets.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", ownerId.value()));
    }

    private long requirePersistedBookingId(Booking booking) {
        if (booking.getId() == null) {
            throw new IllegalStateException("A booking must be persisted before handling its payment");
        }
        return booking.getId();
    }

    private BookablePublication findPublication(long publicationId) {
        return publications.findBookable(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication", publicationId));
    }

    // Una franja tomada se rechaza al pedirla, y no recién al aceptar.
    private static void requireFree(TimeSlot slot) {
        if (slot.isOccupied()) {
            throw new BusinessRuleException("The slot is already taken");
        }
    }

    private TimeSlot findSlot(BookablePublication publication, long slotId) {
        return publication.findSlot(slotId)
                .orElseThrow(() -> new BusinessRuleException("The slot does not belong to the publication"));
    }

    // El repositorio devuelve una copia nueva con id; las transiciones sin avisar quedan en la instancia que se modificó.
    private record Changed(Booking changed, Booking saved) {
    }
}
