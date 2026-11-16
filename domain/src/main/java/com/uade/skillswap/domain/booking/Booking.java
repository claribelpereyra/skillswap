package com.uade.skillswap.domain.booking;

import com.uade.skillswap.domain.booking.observer.BookingEvent;
import com.uade.skillswap.domain.booking.observer.BookingObserver;
import com.uade.skillswap.domain.booking.observer.BookingSubject;
import com.uade.skillswap.domain.booking.state.BookingState;
import com.uade.skillswap.domain.booking.state.BookingStateFactory;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateHistory;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Booking implements BookingSubject {

    private Long id;
    private final long publicationId;
    private final UserId clientId;
    private final UserId providerId;
    private final String message;
    private final String goal;
    private final SkillCoins price;
    private final StateHistory<BookingStatus> history;
    private TimeSlot slot;
    private BookingState state;
    private RescheduleProposal pendingReschedule;
    private final List<BookingObserver> observers = new ArrayList<>();
    // Transiciones de esta instancia que todavía no se avisaron; se publican después de guardar.
    private final List<StateTransition<BookingStatus>> unpublished = new ArrayList<>();

    private Booking(long publicationId, UserId clientId, UserId providerId, TimeSlot slot,
                    String message, String goal, SkillCoins price, StateHistory<BookingStatus> history) {
        this.publicationId = publicationId;
        this.clientId = Objects.requireNonNull(clientId, "The client is required");
        this.providerId = Objects.requireNonNull(providerId, "The provider is required");
        this.slot = Objects.requireNonNull(slot, "The slot is required");
        this.message = message;
        this.goal = goal;
        this.price = Objects.requireNonNull(price, "The price is required");
        this.history = history;
        this.state = BookingStateFactory.fromStatus(history.currentState());
    }

    public static Booking request(long publicationId, UserId clientId, UserId providerId, TimeSlot slot,
                                  String message, String goal, SkillCoins price) {
        Objects.requireNonNull(clientId, "The client is required");
        if (clientId.equals(providerId)) {
            throw new BusinessRuleException("The author cannot request their own publication");
        }
        if (slot == null) {
            throw new BusinessRuleException("A slot is required to request a publication");
        }
        requireText(message, "A message is required to request a publication");
        requireText(goal, "A goal is required to request a publication");
        StateHistory<BookingStatus> history = StateHistory.startingAt(BookingStatus.PENDING, clientId, Instant.now());
        Booking booking = new Booking(publicationId, clientId, providerId, slot, message, goal, price, history);
        booking.unpublished.add(history.last());
        return booking;
    }

    // Reconstruye una reserva guardada; el objeto de estado se arma desde el último estado del historial.
    public static Booking rebuild(Long id, long publicationId, UserId clientId, UserId providerId, TimeSlot slot,
                                  String message, String goal, SkillCoins price,
                                  List<StateTransition<BookingStatus>> history, RescheduleProposal pendingReschedule) {
        Booking booking = new Booking(publicationId, clientId, providerId, slot, message, goal, price,
                StateHistory.rebuild(history));
        booking.id = Objects.requireNonNull(id, "A stored booking must have an id");
        booking.pendingReschedule = pendingReschedule;
        return booking;
    }

    // State Pattern: la reserva delega cada acción en su estado actual.
    public void accept(UserId actor) {
        state.accept(this, actor);
    }

    public void reject(UserId actor, String reason) {
        state.reject(this, actor, reason);
    }

    public void confirmPayment(UserId actor) {
        state.confirmPayment(this, actor);
    }

    public void validatePayment(UserId actor) {
        state.validatePayment(this, actor);
    }

    public void start(UserId actor) {
        state.start(this, actor);
    }

    public void complete(UserId actor) {
        state.complete(this, actor);
    }

    public void cancel(UserId actor, String reason) {
        state.cancel(this, actor, reason);
    }

    public void proposeReschedule(UserId actor, TimeSlot newSlot) {
        state.proposeReschedule(this, actor, newSlot);
    }

    public void acceptReschedule(UserId actor) {
        state.acceptReschedule(this, actor);
    }

    public void rejectReschedule(UserId actor) {
        state.rejectReschedule(this, actor);
    }

    public void changeState(BookingState next, UserId actor, String reason) {
        history.record(state.getStatus(), next.getStatus(), actor, Instant.now(), reason);
        unpublished.add(history.last());
        state = next;
    }

    public boolean isParticipant(UserId user) {
        return clientId.equals(user) || providerId.equals(user);
    }

    public void requireProvider(UserId actor, String action) {
        if (!providerId.equals(actor)) {
            throw new BusinessRuleException("Only the provider can " + action + " a booking");
        }
    }

    public void requireClient(UserId actor, String action) {
        if (!clientId.equals(actor)) {
            throw new BusinessRuleException("Only the client can " + action + " a booking");
        }
    }

    public void requireParticipant(UserId actor, String action) {
        if (!isParticipant(actor)) {
            throw new BusinessRuleException("Only the client or the provider can " + action + " a booking");
        }
    }

    public void requireReason(String reason, String action) {
        requireText(reason, "A reason is required to " + action + " a booking");
    }

    // Lo comparten ACCEPTED y CONFIRMED antes de pasar a CANCELLED.
    public void prepareCancellation(UserId actor, String reason) {
        requireParticipant(actor, "cancel");
        requireReason(reason, "cancel");
        discardReschedule();
    }

    public void registerReschedule(UserId actor, TimeSlot newSlot) {
        if (newSlot == null) {
            throw new BusinessRuleException("A new slot is required to reschedule a booking");
        }
        if (pendingReschedule != null) {
            throw new BusinessRuleException("There is already a pending reschedule for this booking");
        }
        // Sin id no se puede comparar con la franja actual: dos franjas sin guardar parecerían la misma.
        if (newSlot.getId() == null) {
            throw new BusinessRuleException("The new slot must be saved before proposing it");
        }
        if (Objects.equals(newSlot.getId(), slot.getId())) {
            throw new BusinessRuleException("The new slot must be different from the current one");
        }
        pendingReschedule = new RescheduleProposal(newSlot, actor);
    }

    public void acceptPendingReschedule(UserId actor) {
        RescheduleProposal proposal = requirePendingRescheduleAnsweredBy(actor);
        slot = proposal.newSlot();
        recordInCurrentState(actor, "Rescheduled to slot " + slot.getId());
        pendingReschedule = null;
    }

    public void rejectPendingReschedule(UserId actor) {
        RescheduleProposal proposal = requirePendingRescheduleAnsweredBy(actor);
        recordInCurrentState(actor, "Reschedule to slot " + proposal.newSlot().getId() + " rejected");
        pendingReschedule = null;
    }

    public void discardReschedule() {
        pendingReschedule = null;
    }

    private RescheduleProposal requirePendingRescheduleAnsweredBy(UserId actor) {
        if (pendingReschedule == null) {
            throw new BusinessRuleException("There is no pending reschedule for this booking");
        }
        if (pendingReschedule.proposedBy().equals(actor)) {
            throw new BusinessRuleException("The reschedule must be answered by the other participant");
        }
        return pendingReschedule;
    }

    // La reprogramación no cambia el estado: queda en el historial como transición al mismo estado.
    private void recordInCurrentState(UserId actor, String reason) {
        BookingStatus current = state.getStatus();
        history.record(current, current, actor, Instant.now(), reason);
        unpublished.add(history.last());
    }

    @Override
    public void addObserver(BookingObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(BookingObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(BookingEvent event) {
        for (BookingObserver observer : observers) {
            observer.update(event);
        }
    }

    // Se llama después de confirmar la transacción; el id lo pasa quien guardó, porque una reserva nueva no lo tiene.
    public void publishPendingEvents(long bookingId) {
        List<StateTransition<BookingStatus>> toPublish = List.copyOf(unpublished);
        unpublished.clear();
        for (StateTransition<BookingStatus> transition : toPublish) {
            notifyObservers(BookingEvent.of(bookingId, clientId, providerId, transition));
        }
    }

    private static void requireText(String value, String error) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(error);
        }
    }

    public Long getId() {
        return id;
    }

    public long getPublicationId() {
        return publicationId;
    }

    public UserId getClientId() {
        return clientId;
    }

    public UserId getProviderId() {
        return providerId;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public String getMessage() {
        return message;
    }

    public String getGoal() {
        return goal;
    }

    public SkillCoins getPrice() {
        return price;
    }

    public BookingStatus getStatus() {
        return state.getStatus();
    }

    public List<StateTransition<BookingStatus>> getHistory() {
        return history.transitions();
    }

    public Optional<RescheduleProposal> getPendingReschedule() {
        return Optional.ofNullable(pendingReschedule);
    }
}
